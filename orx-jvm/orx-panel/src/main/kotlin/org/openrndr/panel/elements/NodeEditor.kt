package org.openrndr.panel.elements

import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_DELETE
import org.openrndr.KeyModifier
import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import kotlinx.serialization.Serializable
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.Edit
import org.openrndr.panel.PanelText
import org.openrndr.panel.editHistory
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D

/**
 * a port of a [GraphNode], an input or an output, of [type], which only connects to ports of the same type, or any;
 * drawn in [color], and the wires from an output in its color, see [NodeEditor.portColor]
 */
class Port(val name: String, val type: String? = null, val color: ColorRGBa? = null)

/**
 * a node of a [NodeEditor]: its [title], its [inputs] at the left and [outputs] at the right, at [position]; under
 * the ports the [body] that the host draws, and under that the [status]
 */
class GraphNode(
    val title: String,
    val inputs: List<Port> = emptyList(),
    val outputs: List<Port> = emptyList(),
    var position: Vector2 = Vector2.ZERO,
    /** whatever the node stands for */
    val data: Any? = null
) {
    /** the editor the node is in */
    internal var editor: NodeEditor? = null

    /** the width of the node in the coordinates of the graph */
    var width = 150.0
        set(value) { field = value; editor?.requestRedraw() }

    /** a line of text under the node, like an error or a value, in [statusColor] */
    var status: String? = null
        set(value) { if (value != field) { field = value; editor?.requestRedraw() } }

    /** the color of the [status], which also tints the title bar, like red for an error; none draws neither */
    var statusColor: ColorRGBa? = null
        set(value) { if (value != field) { field = value; editor?.requestRedraw() } }

    /**
     * a color the node is drawn in, like for the kind of node it is: it tints the title bar, the outline when the
     * node is neither selected nor highlighted, and faintly the box; a [statusColor] still tints the title bar
     */
    var tint: ColorRGBa? = null
        set(value) { if (value != field) { field = value; editor?.requestRedraw() } }

    /**
     * draws the body of the node, in the box of [bodyHeight] under the ports, in the coordinates of the graph; call
     * [NodeEditor.requestRedraw] when what it draws changes
     */
    var body: ((drawer: Drawer, box: Rectangle) -> Unit)? = null
        set(value) { field = value; editor?.requestRedraw() }

    /** the height of the [body] in the coordinates of the graph */
    var bodyHeight = 0.0
        set(value) { field = value; editor?.requestRedraw() }

    /**
     * handles the pointer in the [body], like for a toggle or a slider drawn there: called with a press in the body,
     * true takes the press, after which the drags and the release go here too, and the node is neither selected nor
     * moved; false lets the press select and move the node. The editor redraws after every call. Changes made here
     * can be undone by recording them in [org.openrndr.panel.editHistory] of the editor.
     */
    var bodyPointer: ((NodeBodyEvent) -> Boolean)? = null

    /**
     * true when the node is drawn compact: the title and the rows of the ports that are connected only, without its
     * body and its status, whose color still tints the title; null follows [NodeEditor.compactNodes]. A compact node
     * shows all its ports while a wire is dragged over it. See [NodeEditor.setCompact] for a change that can be undone
     */
    var compact: Boolean? = null
        set(value) {
            if (value != field) {
                val before = isCompact
                field = value
                if (isCompact != before) editor?.compactChanged(listOf(this))
            }
        }

    /** true when the node is drawn compact now, see [compact] */
    val isCompact: Boolean get() = (compact ?: editor?.compactNodes ?: false) && editor?.expanded !== this

    /** the row of the input or output [i], -1 when it is hidden because the node is compact and it is not connected */
    private fun row(i: Int, isOutput: Boolean): Int {
        val e = editor
        if (!isCompact || e == null) return i
        if (!e.isConnected(this, i, isOutput)) return -1
        return (0 until i).count { e.isConnected(this, it, isOutput) }
    }

    /** the number of rows of the inputs or the outputs that are shown */
    internal fun shownPorts(isOutput: Boolean): Int {
        val size = if (isOutput) outputs.size else inputs.size
        val e = editor
        if (!isCompact || e == null) return size
        return (0 until size).count { e.isConnected(this, it, isOutput) }
    }

    /** true when the input [i] is shown, false when the node is compact and the input is not connected */
    fun isInputShown(i: Int) = row(i, false) >= 0

    /** true when the output [i] is shown, false when the node is compact and the output is not connected */
    fun isOutputShown(i: Int) = row(i, true) >= 0

    private val portsHeight: Double
        get() {
            val rows = maxOf(shownPorts(false), shownPorts(true))
            return if (rows == 0 && isCompact) 0.0 else rows * NodeEditor.PORT_SPACING + 8.0
        }

    /** true when the body is drawn: there is one and the node is not compact */
    internal val hasBody: Boolean get() = body != null && !isCompact

    /** true when the status is drawn: there is one and the node is not compact */
    internal val hasStatus: Boolean get() = status != null && !isCompact

    val height: Double get() = NodeEditor.TITLE_HEIGHT + portsHeight + (if (hasBody) bodyHeight else 0.0) +
            (if (hasStatus) NodeEditor.STATUS_HEIGHT else 0.0)
    val bounds: Rectangle get() = Rectangle(position, width, height)

    /** the box of the [body] in the coordinates of the graph, without height when it is not drawn */
    val bodyBounds: Rectangle get() = Rectangle(position.x, position.y + NodeEditor.TITLE_HEIGHT + portsHeight, width, if (hasBody) bodyHeight else 0.0)

    /** the y of the row [r], the middle of the title bar for a hidden port */
    private fun rowY(r: Int) = if (r < 0) NodeEditor.TITLE_HEIGHT / 2.0 else NodeEditor.TITLE_HEIGHT + (r + 0.5) * NodeEditor.PORT_SPACING + 4.0

    /** the position of the input [i] in the coordinates of the graph, at the left of the title bar when it is hidden */
    fun inputPosition(i: Int) = position + Vector2(0.0, rowY(row(i, false)))
    /** the position of the output [i] in the coordinates of the graph, at the right of the title bar when it is hidden */
    fun outputPosition(i: Int) = position + Vector2(width, rowY(row(i, true)))
}

/**
 * a pointer event in the body of [node], see [GraphNode.bodyPointer]: a press, a drag or a release, at [position] in
 * the coordinates of the graph, like [box], the box of the body; [fraction] is where it is in the box, from 0 to 1
 */
class NodeBodyEvent(
    val node: GraphNode,
    val type: MouseEventType,
    val position: Vector2,
    val box: Rectangle,
    val button: MouseButton,
    val modifiers: Set<KeyModifier>
) {
    val fraction: Vector2 get() = Vector2((position.x - box.x) / box.width, (position.y - box.y) / box.height)
}

/**
 * a marker on a [GraphNode], see [NodeEditor.badges]: a dot in [color], or a pill with [label] in it, like a count;
 * [tooltip] shows when the pointer rests on it, [key] tells it from the other badges of the node, by default its index
 */
data class NodeBadge(val color: ColorRGBa, val label: String? = null, val tooltip: String? = null, val key: Any? = null)

/**
 * a click on [badge] of [node], see [NodeEditor.Events.badgeClicked]: [area] is where the badge is in the window, and
 * [screenPosition] its middle, like for a [popover] there
 */
class BadgeClicked(val node: GraphNode, val badge: NodeBadge, val area: Rectangle) {
    val screenPosition: Vector2 get() = area.center
}

/** a wire from the output [output] of [from] to the input [input] of [to] */
data class Wire(val from: GraphNode, val output: Int, val to: GraphNode, val input: Int)

/** a saved [NodeEditor], see [NodeEditor.graph] and [NodeEditor.restore]; the wires refer to the nodes by index */
@Serializable
data class NodeGraphModel(val nodes: List<NodeModel> = emptyList(), val wires: List<WireModel> = emptyList())

/** a saved [GraphNode], with the [id] of what it stands for */
@Serializable
data class NodeModel(
    val title: String,
    val x: Double,
    val y: Double,
    val width: Double = 150.0,
    val inputs: List<PortModel> = emptyList(),
    val outputs: List<PortModel> = emptyList(),
    val id: String? = null,
    val compact: Boolean? = null
)

/** a saved [Port] */
@Serializable
data class PortModel(val name: String, val type: String? = null, val color: ColorRGBa? = null)

/** a saved [Wire], from the output [output] of the node [from] to the input [input] of the node [to] */
@Serializable
data class WireModel(val from: Int, val output: Int, val to: Int, val input: Int)

/**
 * A graph of [nodes] with ports, connected by [wires]. Dragging a node moves it with the other selected nodes, dragging
 * from an output draws a wire that connects to the input it is released on, dragging a wire off an input detaches it;
 * an input has one wire at most. A click selects a node, with shift more; dragging on the background selects the nodes
 * in the box, and delete removes the selected nodes. The wheel zooms around the pointer, dragging with the middle
 * button or with alt pans. The changes can be undone, see [org.openrndr.panel.History]. Nodes are drawn in the colors
 * of the style of the editor, the selected ones and wires in that of [accent], ports and their wires in [portColor].
 *
 * Every change of [nodes] and [wires] fires an event, also those of undo and redo and those of the host: [Events.added]
 * and [Events.removed] for nodes, [Events.connected] and [Events.disconnected] for wires, [Events.moved] for moves, so
 * a model can follow the editor by its events alone.
 */
class NodeEditor : Element(ElementType("node-editor")) {
    override val handlesKeyboardFocus = true
    override val handlesDoubleClick = true

    companion object {
        const val TITLE_HEIGHT = 26.0
        const val PORT_SPACING = 22.0
        const val STATUS_HEIGHT = 20.0

        /** the height of a badge, and the width of one without a label, in pixels */
        const val BADGE_SIZE = 14.0

        /** the distance between the dots of the grid, in the coordinates of the graph */
        const val GRID_SPACING = 24.0
    }

    private val gridStyle = shadeStyle {
        fragmentTransform = """
            // the point of the graph under the fragment
            vec2 local = c_boundsPosition.xy * p_size;
            vec2 g = (local - p_pan) / p_zoom;
            // the distance to the nearest dot, in pixels
            vec2 nearest = floor(g / p_spacing + 0.5) * p_spacing;
            float d = length(g - nearest) * p_zoom;
            // dots are a world size, but at least a pixel, and fade out where they get too close together
            float radius = max(1.1 * p_zoom, 0.8);
            float coverage = 1.0 - smoothstep(radius - 0.6, radius + 0.6, d);
            float density = smoothstep(5.0, 10.0, p_spacing * p_zoom);
            x_fill.a *= coverage * density;
        """
    }

    class Events : AutoCloseable {
        /** a wire was connected */
        val connected = Event<Wire>("node-editor-connected")

        /** a wire was disconnected */
        val disconnected = Event<Wire>("node-editor-disconnected")

        /** nodes were added, by [add] or by undoing their removal; fired before the [connected] of their wires */
        val added = Event<List<GraphNode>>("node-editor-added")

        /** nodes were removed, by [remove] or by undoing their addition; fired after the [disconnected] of their wires */
        val removed = Event<List<GraphNode>>("node-editor-removed")

        /** nodes were moved, at the end of a drag, or by its undo or redo */
        val moved = Event<List<GraphNode>>("node-editor-moved")

        /**
         * nodes became compact or were expanded, see [GraphNode.compact], which changes their heights and the positions
         * of their ports; also by undo and redo, and by [compactNodes]
         */
        val compactChanged = Event<List<GraphNode>>("node-editor-compact-changed")

        /** the selected nodes changed */
        val selected = Event<Set<GraphNode>>("node-editor-selected")

        /** the background was double clicked, at the position in the coordinates of the graph, like to add a node */
        val requested = Event<Vector2>("node-editor-requested")

        /** a badge of a node was clicked, see [badges] */
        val badgeClicked = Event<BadgeClicked>("node-editor-badge-clicked")

        override fun close() {
            connected.close(); disconnected.close(); added.close(); removed.close(); moved.close(); compactChanged.close(); selected.close(); requested.close()
            badgeClicked.close()
        }
    }

    val events = Events()

    private val nodeList = mutableListOf<GraphNode>()
    private val wireList = mutableListOf<Wire>()

    /** the nodes, the last drawn on top; changed by [add] and [remove] */
    val nodes: List<GraphNode> get() = nodeList

    /** the wires; changed by [connect] and [disconnect] */
    val wires: List<Wire> get() = wireList

    /** the nodes that are selected */
    var selection: Set<GraphNode> = emptySet()
        set(value) {
            if (value != field) {
                field = value
                requestRedraw()
                events.selected.trigger(value)
            }
        }

    /** the offset of the graph in the element, in pixels */
    var pan = Vector2.ZERO
        set(value) {
            field = value
            requestRedraw()
        }

    /** the scale of the graph */
    var zoom = 1.0
        set(value) {
            field = value.coerceIn(0.2, 4.0)
            requestRedraw()
        }

    /** the color of selections and wires */
    var accent: ColorRGBa = ColorRGBa.fromHex("#5ab0ff")

    /**
     * Records an edit of this element, named [name], in the history so that it can be undone and redone, by default
     * in [org.openrndr.panel.editHistory]. A program can take this over, like to wrap [undo] and [redo] so that they
     * leave alone what others changed since, or to keep the edits in a history of its own.
     */
    var recordEdit: (name: String, undo: () -> Unit, redo: () -> Unit) -> Unit = { name, undo, redo ->
        editHistory?.record(Edit(name, null, undo, redo))
    }

    /** true when hovering over an input highlights its upstream: the wires and nodes that lead to it, through all inputs */
    var highlightUpstream = false
        set(value) { field = value; updateHighlight() }

    /** true when hovering over an output highlights its downstream: the wires and nodes it leads to, through all outputs */
    var highlightDownstream = false
        set(value) { field = value; updateHighlight() }

    /** the color of the outlines of the nodes that are highlighted, see [highlightUpstream] and [highlightDownstream] */
    var highlightColor: ColorRGBa = ColorRGBa.fromHex("#ffd166")

    /** the nodes and wires that are highlighted now, empty when nothing is */
    var highlightedNodes: Set<GraphNode> = emptySet()
        private set
    var highlightedWires: Set<Wire> = emptySet()
        private set

    /** the port that the pointer is over, with true for an output */
    private var hovered: Triple<GraphNode, Int, Boolean>? = null

    /** the wires and nodes upstream of the input [input] of [node], or downstream of its output [output] */
    private fun stream(node: GraphNode, port: Int, isOutput: Boolean): Pair<Set<GraphNode>, Set<Wire>> {
        val nodes = LinkedHashSet<GraphNode>()
        val found = LinkedHashSet<Wire>()
        val queue = ArrayDeque<Wire>()
        queue.addAll(wireList.filter { if (isOutput) it.from === node && it.output == port else it.to === node && it.input == port })
        while (queue.isNotEmpty()) {
            val w = queue.removeFirst()
            if (!found.add(w)) continue
            val next = if (isOutput) w.to else w.from
            // a node that was reached goes on through all of its outputs, or all of its inputs
            if (nodes.add(next)) queue.addAll(wireList.filter { if (isOutput) it.from === next else it.to === next })
        }
        return nodes to found
    }

    private fun updateHighlight() {
        val h = hovered?.takeIf { (_, _, isOutput) -> gesture == null && (if (isOutput) highlightDownstream else highlightUpstream) }
        val (nodes, wires) = h?.let { (n, p, o) -> stream(n, p, o) } ?: (emptySet<GraphNode>() to emptySet())
        if (nodes != highlightedNodes || wires != highlightedWires) {
            highlightedNodes = nodes
            highlightedWires = wires
            requestRedraw()
        }
    }

    /** true when a wire can go from [output] to [input], by default when their types are the same or either has none */
    var canConnect: (output: Port, input: Port) -> Boolean = { o, i -> o.type == null || i.type == null || o.type == i.type }

    /** the color of a port and of the wires from it, by default its own; none draws it in [accent] */
    var portColor: (Port) -> ColorRGBa? = { it.color }
        set(value) { field = value; requestRedraw() }

    /**
     * a text drawn after the name of a port, like its current value, or none; call [requestRedraw] when the texts
     * change
     */
    var portLabel: (node: GraphNode, port: Port, isOutput: Boolean) -> String? = { _, _, _ -> null }
        set(value) { field = value; requestRedraw() }

    /**
     * the points, in the coordinates of the graph, that a wire goes through on its way from its output to its input,
     * like to go around nodes, or none for the direct way; drawn as one smooth curve that leaves the output and enters
     * the input horizontally. Call [requestRedraw] when the routes change
     */
    var wireRoute: (Wire) -> List<Vector2>? = { null }
        set(value) { field = value; requestRedraw() }

    /**
     * the badges of a node, drawn next to each other on its top right corner, at the same size at every zoom, and over
     * the nodes; a press on a badge neither selects nor moves the node, a click fires [Events.badgeClicked]. Call
     * [requestRedraw] when the badges change
     */
    var badges: (GraphNode) -> List<NodeBadge> = { emptyList() }
        set(value) { field = value; requestRedraw() }

    /** the style of the labels of badges */
    private val badgeStyle = StyleSheet().apply { fontSize = 10.px }

    /** the badges of [n] with their areas in the element, the last at the right */
    private fun badgeAreas(n: GraphNode): List<Pair<NodeBadge, Rectangle>> {
        val list = badges(n)
        if (list.isEmpty()) return emptyList()
        val text = panelText
        val corner = local(Vector2(n.position.x + n.width, n.position.y))
        var right = corner.x - 8.0
        val result = ArrayList<Pair<NodeBadge, Rectangle>>(list.size)
        for (b in list.asReversed()) {
            val width = b.label?.let { label -> maxOf(BADGE_SIZE, (text?.width(badgeStyle, label) ?: 0.0) + 8.0) } ?: BADGE_SIZE
            result.add(b to Rectangle(right - width, corner.y - BADGE_SIZE / 2.0, width, BADGE_SIZE))
            right -= width + 3.0
        }
        return result.asReversed()
    }

    /**
     * the area of the window of the badge of [node] with [key], or with that index when the badges have no keys; null
     * when it has none, like when the node is removed, or when the badge is out of view of the editor. For a [popover]
     * that follows the badge when the graph pans and zooms, and hides while it is out of view:
     * `cm.popover({ editor.badgeArea(node, key) }) { ... }`
     */
    fun badgeArea(node: GraphNode, key: Any): Rectangle? {
        if (node !in nodeList) return null
        val areas = badgeAreas(node)
        val area = areas.withIndex().firstOrNull { (i, a) -> (a.first.key ?: i) == key }?.value?.second ?: return null
        if (!area.intersects(Rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight))) return null
        return area.moved(Vector2(layout.screenX, layout.screenY))
    }

    /** the badge at [p], a point of the window, with its node and its index; the badges are over the nodes */
    private fun badgeAt(p: Vector2): Triple<GraphNode, Int, NodeBadge>? {
        val q = p - Vector2(layout.screenX, layout.screenY)
        for (n in nodes.asReversed()) {
            val areas = badgeAreas(n)
            val i = areas.indexOfLast { q in it.second }
            if (i >= 0) return Triple(n, i, areas[i].first)
        }
        return null
    }

    /** the tooltip of the badge under the pointer, or the tooltip of the editor */
    override var tooltip: String?
        get() = (root() as? Body)?.controlManager?.mousePosition?.let { p -> badgeAt(p)?.third?.tooltip } ?: super.tooltip
        set(value) { super.tooltip = value }

    /** true when the nodes whose [GraphNode.compact] is null are drawn compact */
    var compactNodes = false
        set(value) {
            if (value != field) {
                val following = nodeList.filter { it.compact == null && it !== expanded }
                field = value
                compactChanged(following)
            }
        }

    /** the compact node that shows all its ports, because a wire is dragged over it */
    internal var expanded: GraphNode? = null
        set(value) {
            if (value !== field) {
                field = value
                requestRedraw()
            }
        }

    internal fun compactChanged(changed: List<GraphNode>) {
        requestRedraw()
        if (changed.isNotEmpty()) events.compactChanged.trigger(changed)
    }

    /**
     * makes [changed] compact when [compact] is true, expands them when it is false, or lets them follow [compactNodes]
     * when it is null; the change can be undone
     */
    fun setCompact(changed: Collection<GraphNode>, compact: Boolean?) {
        val before = changed.associateWith { it.compact }
        if (before.values.all { it == compact }) return
        for (n in changed) n.compact = compact
        recordEdit(if (compact == false) "Expand nodes" else "Compact nodes",
            { for ((n, c) in before) n.compact = c },
            { for (n in changed) n.compact = compact })
    }

    /** the inputs and outputs that are connected, as of [wiresVersion] */
    private var wiresVersion = 0
    private var connectedVersion = -1
    private val connectedIn = HashSet<Pair<GraphNode, Int>>()
    private val connectedOut = HashSet<Pair<GraphNode, Int>>()

    internal fun isConnected(n: GraphNode, i: Int, isOutput: Boolean): Boolean {
        if (connectedVersion != wiresVersion) {
            connectedIn.clear(); connectedOut.clear()
            for (w in wireList) { connectedIn.add(w.to to w.input); connectedOut.add(w.from to w.output) }
            connectedVersion = wiresVersion
        }
        return (n to i) in (if (isOutput) connectedOut else connectedIn)
    }

    /** adds [node] at [position] in the coordinates of the graph */
    fun add(node: GraphNode, position: Vector2 = node.position): GraphNode {
        if (node in nodeList) return node
        node.position = position
        node.editor = this
        nodeList.add(node)
        requestRedraw()
        events.added.trigger(listOf(node))
        return node
    }

    /** connects the output [output] of [from] to the input [input] of [to], replacing the wire at that input */
    fun connect(from: GraphNode, output: Int, to: GraphNode, input: Int): Wire? {
        if (from === to || !canConnect(from.outputs[output], to.inputs[input])) return null
        if (from !in nodeList || to !in nodeList) return null
        val wire = Wire(from, output, to, input)
        if (wire in wireList) return wire
        attach(wire)
        return wire
    }

    /** adds [wire] whether it can connect or not, like when an edit is undone, replacing the wire at its input */
    private fun attach(wire: Wire) {
        if (wire in wireList || wire.from !in nodeList || wire.to !in nodeList) return
        wireList.filter { it.to === wire.to && it.input == wire.input }.forEach { disconnect(it) }
        wireList.add(wire)
        wiresVersion++
        updateHighlight()
        requestRedraw()
        events.connected.trigger(wire)
    }

    /** removes [wire] */
    fun disconnect(wire: Wire) {
        if (wireList.remove(wire)) {
            wiresVersion++
            updateHighlight()
            requestRedraw()
            events.disconnected.trigger(wire)
        }
    }

    /** removes [removed] and their wires */
    fun remove(removed: Collection<GraphNode>) {
        val set = removed.filter { it in nodeList }.toSet()
        if (set.isEmpty()) return
        wireList.filter { it.from in set || it.to in set }.forEach { disconnect(it) }
        nodeList.removeAll(set)
        for (n in set) n.editor = null
        selection = selection - set
        requestRedraw()
        events.removed.trigger(set.toList())
    }

    /** puts [restored] back at the places they had in [order], with [lost], their wires, like when a removal is undone */
    private fun reinsert(restored: Collection<GraphNode>, order: List<GraphNode>, lost: List<Wire>) {
        val back = restored.filter { it !in nodeList }
        if (back.isEmpty()) return
        val before = order.withIndex().associate { (i, n) -> n to i }
        for (n in back.sortedBy { before[it] ?: Int.MAX_VALUE }) {
            // after the last node that was before it
            val i = before[n] ?: nodeList.size
            val at = nodeList.indexOfLast { (before[it] ?: Int.MAX_VALUE) < i } + 1
            n.editor = this
            nodeList.add(at, n)
        }
        requestRedraw()
        events.added.trigger(back)
        for (w in lost) attach(w)
    }

    //<editor-fold desc="saving and restoring">
    /** the nodes and wires as they are now, [id] gives the [NodeModel.id] of a node, by default its data as a string */
    fun graph(id: (GraphNode) -> String? = { it.data?.toString() }): NodeGraphModel {
        val index = nodeList.withIndex().associate { (i, n) -> n to i }
        fun port(p: Port) = PortModel(p.name, p.type, p.color)
        return NodeGraphModel(
            nodeList.map { n -> NodeModel(n.title, n.position.x, n.position.y, n.width, n.inputs.map(::port), n.outputs.map(::port), id(n), n.compact) },
            wireList.map { w -> WireModel(index.getValue(w.from), w.output, index.getValue(w.to), w.input) }
        )
    }

    /**
     * Replaces the nodes and wires by those of [model], firing the events of the removals and the additions; [create]
     * makes the node of a [NodeModel], by default with its ports and with its id as data, or none to leave it out.
     * Wires that do not fit their nodes, or that [canConnect] refuses, are left out. Not recorded in the history.
     * @return the nodes, by the index of their models
     */
    fun restore(
        model: NodeGraphModel,
        create: (NodeModel) -> GraphNode? = { m ->
            GraphNode(m.title, m.inputs.map { Port(it.name, it.type, it.color) }, m.outputs.map { Port(it.name, it.type, it.color) }, data = m.id)
        }
    ): List<GraphNode?> {
        remove(nodeList.toList())
        val created = model.nodes.map { m -> create(m)?.also { it.width = m.width; it.compact = m.compact; add(it, Vector2(m.x, m.y)) } }
        for (w in model.wires) {
            val from = created.getOrNull(w.from) ?: continue
            val to = created.getOrNull(w.to) ?: continue
            if (w.output in from.outputs.indices && w.input in to.inputs.indices) connect(from, w.output, to, w.input)
        }
        return created
    }
    //</editor-fold>

    //<editor-fold desc="coordinates">
    /** [p], a point of the window, in the coordinates of the graph */
    fun graphPosition(p: Vector2): Vector2 = (p - Vector2(layout.screenX, layout.screenY) - pan) / zoom

    private fun local(g: Vector2) = g * zoom + pan

    private fun nodeAt(g: Vector2) = nodes.lastOrNull { g in it.bounds }

    /** the node whose body, that handles the pointer, is at [g], when no other node is over it */
    private fun bodyAt(g: Vector2) = nodeAt(g)?.takeIf { it.hasBody && it.bodyPointer != null && g in it.bodyBounds }

    /** passes [e], of [type], to the body of [n] */
    private fun toBody(n: GraphNode, type: MouseEventType, g: Vector2, e: MouseEvent, button: MouseButton = e.button): Boolean {
        val handled = n.bodyPointer?.invoke(NodeBodyEvent(n, type, g, n.bodyBounds, button, e.modifiers)) == true
        requestRedraw()
        return handled
    }

    private fun outputAt(g: Vector2): Pair<GraphNode, Int>? = nodes.asReversed().firstNotNullOfOrNull { n ->
        n.outputs.indices.firstOrNull { n.isOutputShown(it) && n.outputPosition(it).distanceTo(g) < 9.0 }?.let { n to it }
    }

    private fun inputAt(g: Vector2): Pair<GraphNode, Int>? = nodes.asReversed().firstNotNullOfOrNull { n ->
        n.inputs.indices.firstOrNull { n.isInputShown(it) && n.inputPosition(it).distanceTo(g) < 9.0 }?.let { n to it }
    }
    //</editor-fold>

    //<editor-fold desc="the pointer">
    private sealed class Gesture {
        class Move(val start: Vector2, val from: Map<GraphNode, Vector2>) : Gesture()
        class Connect(val from: GraphNode, val output: Int, var end: Vector2, val detached: Wire?) : Gesture()
        class Select(val start: Vector2, var end: Vector2, val before: Set<GraphNode>) : Gesture()
        class Pan(val start: Vector2, val from: Vector2) : Gesture()
        class Body(val node: GraphNode, val button: MouseButton) : Gesture()
        class Badge(val node: GraphNode, val key: Any) : Gesture()
    }

    private var gesture: Gesture? = null

    /** the wire that is drawn while it is dragged, in the coordinates of the graph */
    private val dragging get() = gesture as? Gesture.Connect

    /** true when dragging nodes, a wire or a selection box near or past the edges of the editor pans the view */
    var edgePan = true

    /** the distance from the edges, in pixels, within which a drag pans the view */
    var edgePanMargin = 24.0

    /** how fast the view pans, in pixels per second, with the pointer at an edge; it pans faster further past it */
    var edgePanSpeed = 600.0

    /** the last drag event, which is applied again while the view pans under a pointer that stands still */
    private var lastDrag: MouseEvent? = null
    private var lastPanTime = 0L

    /** follows a drag to [e]: the nodes, the end of the wire or the box go where the pointer is */
    private fun dragTo(e: MouseEvent) {
        lastDrag = e
        val g = graphPosition(e.position)
        when (val current = gesture) {
            is Gesture.Pan -> pan = current.from + (e.position - current.start)
            is Gesture.Move -> {
                val d = g - current.start
                for ((n, p) in current.from) n.position = p + d
                requestRedraw()
            }
            is Gesture.Connect -> {
                current.end = g
                // a compact node under the wire shows all its ports, to connect to one that is hidden
                expanded = nodeAt(g)?.takeIf { n -> n === expanded || n.isCompact }
                requestRedraw()
            }
            is Gesture.Body -> toBody(current.node, MouseEventType.DRAGGED, g, e, current.button)
            is Gesture.Select -> {
                current.end = g
                val box = Rectangle.fromCenter((current.start + g) / 2.0, kotlin.math.abs(g.x - current.start.x), kotlin.math.abs(g.y - current.start.y))
                selection = current.before + nodes.filter { n -> n.bounds.intersects(box) }
                // the box follows the pointer also when the selection stays the same
                requestRedraw()
            }
            is Gesture.Badge, null -> {}
        }
    }

    /**
     * pans the view while nodes, a wire or a box are dragged near or past the edges, by the time since the last frame,
     * and applies the drag again so that what is dragged stays under the pointer
     */
    private fun edgePanStep() {
        val now = System.nanoTime()
        val dt = ((now - lastPanTime) / 1E9).coerceIn(0.0, 0.05)
        lastPanTime = now
        val e = lastDrag ?: return
        if (!edgePan || (gesture !is Gesture.Move && gesture !is Gesture.Connect && gesture !is Gesture.Select)) return
        val local = e.position - Vector2(layout.screenX, layout.screenY)
        val m = edgePanMargin.coerceAtLeast(1.0)
        // 0 at the inner side of the margin, 1 at the edge, more past it, up to 4
        fun depth(near: Double, far: Double) = when {
            near < m -> ((m - near) / m).coerceAtMost(4.0)
            far < m -> -((m - far) / m).coerceAtMost(4.0)
            else -> 0.0
        }
        val v = Vector2(depth(local.x, layout.screenWidth - local.x), depth(local.y, layout.screenHeight - local.y))
        if (v == Vector2.ZERO) return
        // the view moves towards the edge: the graph moves the other way
        pan += v * edgePanSpeed * dt
        dragTo(e)
    }

    override fun prepare() {
        edgePanStep()
    }

    init {
        mouse.pressed.listen {
            val g = graphPosition(it.position)
            val shift = KeyModifier.SHIFT in it.modifiers
            val badge = if (it.button == MouseButton.LEFT) badgeAt(it.position) else null
            gesture = when {
                it.button == MouseButton.CENTER || KeyModifier.ALT in it.modifiers -> Gesture.Pan(it.position, pan)
                badge != null -> Gesture.Badge(badge.first, badge.third.key ?: badge.second)
                outputAt(g) != null -> outputAt(g)!!.let { (n, o) -> Gesture.Connect(n, o, g, null) }
                bodyAt(g)?.let { n -> toBody(n, MouseEventType.BUTTON_DOWN, g, it) } == true -> Gesture.Body(bodyAt(g)!!, it.button)
                inputAt(g)?.let { (n, i) -> wires.firstOrNull { w -> w.to === n && w.input == i } } != null -> {
                    // the wire is taken off the input and follows the pointer
                    val (n, i) = inputAt(g)!!
                    val wire = wires.first { w -> w.to === n && w.input == i }
                    // a compact node keeps showing the input while the wire is over it
                    if (n.isCompact) expanded = n
                    disconnect(wire)
                    Gesture.Connect(wire.from, wire.output, g, wire)
                }
                nodeAt(g) != null -> {
                    val node = nodeAt(g)!!
                    selection = when {
                        shift -> if (node in selection) selection - node else selection + node
                        node in selection -> selection
                        else -> setOf(node)
                    }
                    // the node is drawn over the others
                    nodeList.remove(node); nodeList.add(node)
                    Gesture.Move(g, selection.associateWith { n -> n.position })
                }
                else -> {
                    if (!shift) selection = emptySet()
                    Gesture.Select(g, g, selection)
                }
            }
            // no highlighting during a gesture
            updateHighlight()
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            dragTo(it)
            it.cancelPropagation()
        }
        mouse.clicked.listen { it.cancelPropagation() }
        mouse.released.listen {
            val g = graphPosition(it.position)
            (gesture as? Gesture.Body)?.let { b -> toBody(b.node, MouseEventType.BUTTON_UP, g, it, b.button) }
            // a click on a badge is a release on the badge it was pressed on
            (gesture as? Gesture.Badge)?.let { b ->
                badgeAt(it.position)?.takeIf { (n, i, badge) -> n === b.node && (badge.key ?: i) == b.key }?.let { (n, i, badge) ->
                    val area = badgeAreas(n)[i].second.moved(Vector2(layout.screenX, layout.screenY))
                    events.badgeClicked.trigger(BadgeClicked(n, badge, area))
                }
            }
            finish(g)
        }
        mouse.doubleClicked.listen {
            val g = graphPosition(it.position)
            if (nodeAt(g) == null) events.requested.trigger(g)
            it.cancelPropagation()
        }
        // the port under the pointer, for highlighting its upstream or downstream
        mouse.moved.listen {
            val g = graphPosition(it.position)
            hovered = outputAt(g)?.let { (n, o) -> Triple(n, o, true) } ?: inputAt(g)?.let { (n, i) -> Triple(n, i, false) }
            updateHighlight()
        }
        mouse.exited.listen {
            hovered = null
            updateHighlight()
        }
        mouse.scrolled.listen {
            // the point under the pointer stays where it is
            val g = graphPosition(it.position)
            zoom *= Math.pow(1.1, it.rotation.y)
            pan = it.position - Vector2(layout.screenX, layout.screenY) - g * zoom
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            if ((it.key == KEY_DELETE || it.key == KEY_BACKSPACE) && selection.isNotEmpty()) {
                removeWithUndo(selection)
                it.cancelPropagation()
            }
        }
    }

    private fun finish(g: Vector2) {
        lastDrag = null
        when (val current = gesture) {
            is Gesture.Move -> {
                val to = current.from.keys.associateWith { it.position }
                if (to.any { (n, p) -> p != current.from[n] }) {
                    val moved = to.keys.toList()
                    events.moved.trigger(moved)
                    recordEdit("Move nodes",
                        { for ((n, p) in current.from) n.position = p; requestRedraw(); events.moved.trigger(moved) },
                        { for ((n, p) in to) n.position = p; requestRedraw(); events.moved.trigger(moved) })
                }
            }
            is Gesture.Connect -> {
                val target = inputAt(g)
                val replaced = target?.let { (n, i) -> wires.firstOrNull { w -> w.to === n && w.input == i } }
                val wire = target?.let { (n, i) -> connect(current.from, current.output, n, i) }
                val detached = current.detached
                if (wire != detached) {
                    recordEdit(if (wire != null) "Connect" else "Disconnect", {
                        wire?.let { disconnect(it) }
                        replaced?.let { attach(it) }
                        detached?.let { attach(it) }
                        requestRedraw()
                    }, {
                        detached?.let { disconnect(it) }
                        wire?.let { attach(it) }
                    })
                }
                requestRedraw()
            }
            else -> {}
        }
        gesture = null
        expanded = null
        updateHighlight()
    }

    private fun removeWithUndo(removed: Set<GraphNode>) {
        val lostWires = wireList.filter { it.from in removed || it.to in removed }
        val order = nodeList.toList()
        remove(removed)
        recordEdit("Remove nodes", {
            reinsert(removed, order, lostWires)
            selection = removed
        }, { remove(removed) })
    }
    //</editor-fold>

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        drawer.isolated {
            // the dots of the grid, at points of the graph, so that they stay where they are while the graph zooms
            // and pans; drawn by a shade style over the whole editor
            drawer.isolated {
                drawer.stroke = null
                drawer.fill = color.opacify(0.16)
                drawer.shadeStyle = gridStyle
                gridStyle.parameter("size", Vector2(layout.screenWidth, layout.screenHeight))
                gridStyle.parameter("pan", pan)
                gridStyle.parameter("zoom", zoom)
                gridStyle.parameter("spacing", GRID_SPACING)
                drawer.rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight)
            }
            drawer.translate(pan)
            drawer.scale(zoom)
            // the wires under the nodes, a line strip per wire in a single draw per color
            drawer.fill = null
            drawer.lineCap = LineCap.ROUND
            drawer.strokeWeight = 2.0
            val tolerance = 0.25 / zoom
            fun wire(a: Vector2, b: Vector2, route: List<Vector2>? = null): List<Vector2> {
                val points = listOf(a) + route.orEmpty() + listOf(b)
                // horizontal at the ports, through the waypoints in the direction from the point before to the one after
                val tangents = points.indices.map { i ->
                    if (i == 0 || i == points.lastIndex) Vector2.UNIT_X
                    else (points[i + 1] - points[i - 1]).let { d -> if (d.length < 1E-9) Vector2.UNIT_X else d.normalized }
                }
                val out = mutableListOf<Vector2>()
                for (i in 0 until points.lastIndex) {
                    val p = points[i]
                    val q = points[i + 1]
                    // the reach of the handles: half the width of a direct wire, at least 40, and a third of the
                    // distance between the points of a route, at least 20 at the ports
                    val (dp, dq) = if (points.size == 2) maxOf(40.0, kotlin.math.abs(q.x - p.x) * 0.5).let { it to it } else {
                        val span = q.distanceTo(p) / 3.0
                        (if (i == 0) maxOf(span, 20.0) else span) to (if (i + 1 == points.lastIndex) maxOf(span, 20.0) else span)
                    }
                    val positions = Segment2D(p, p + tangents[i] * dp, q - tangents[i + 1] * dq, q).adaptivePositions(tolerance)
                    out.addAll(if (i == 0) positions else positions.drop(1))
                }
                return out
            }
            fun wireColor(from: GraphNode, output: Int) = portColor(from.outputs[output]) ?: accent
            // while a stream is highlighted its wires are full and thicker, the others faint
            val highlighting = highlightedWires.isNotEmpty()
            fun wireStyle(w: Wire): Pair<ColorRGBa, Double> {
                val c = wireColor(w.from, w.output)
                return when {
                    !highlighting -> c.opacify(0.8) to 2.0
                    w in highlightedWires -> c to 3.0
                    else -> c.opacify(0.2) to 2.0
                }
            }
            val strips = wires.groupBy(::wireStyle).mapValues { (_, ws) ->
                ws.map { w -> wire(w.from.outputPosition(w.output), w.to.inputPosition(w.input), wireRoute(w)) }
            }.toMutableMap()
            dragging?.let {
                val style = wireColor(it.from, it.output) to 2.0
                strips[style] = strips[style].orEmpty() + listOf(wire(it.from.outputPosition(it.output), inputAt(it.end)?.let { (n, i) -> n.inputPosition(i) } ?: it.end))
            }
            for ((style, s) in strips) {
                drawer.stroke = style.first
                drawer.strokeWeight = style.second
                drawer.lineStrips(s)
            }
            // the nodes, in layers of nodes that do not overlap, each layer in a few draws: the boxes, the dots of the
            // ports, the texts that stay and the labels of the ports, then the bodies
            val portText = color.opacify(0.75)
            val labelText = color.opacify(0.45)
            val connectedInputs = wires.mapTo(HashSet()) { it.to to it.input }
            val connectedOutputs = wires.mapTo(HashSet()) { it.from to it.output }
            val layers = mutableListOf<MutableList<GraphNode>>()
            val layerOf = HashMap<GraphNode, Int>()
            for ((j, n) in nodes.withIndex()) {
                // the ports and the stroke of the selection reach over the bounds
                val reach = n.bounds.offsetEdges(8.0)
                var layer = 0
                for (i in 0 until j) {
                    val m = nodes[i]
                    if (layerOf.getValue(m) >= layer && m.bounds.offsetEdges(8.0).intersects(reach)) layer = layerOf.getValue(m) + 1
                }
                layerOf[n] = layer
                if (layer == layers.size) layers.add(mutableListOf())
                layers[layer].add(n)
            }
            val nodeFill = ColorRGBa.fromHex("#2a2e36")
            for ((l, layer) in layers.withIndex()) {
                drawer.rectangles {
                    for (n in layer) {
                        val selected = n in selection
                        val highlighted = n in highlightedNodes
                        val tint = n.tint
                        stroke = when {
                            selected -> accent
                            highlighted -> highlightColor
                            tint != null -> tint.opacify(0.45)
                            else -> color.opacify(0.25)
                        }
                        strokeWeight = if (selected || highlighted) 2.0 else 1.0
                        fill = if (tint != null) nodeFill.mix(tint, 0.08) else nodeFill
                        rectangle(n.bounds)
                        // the title bar, tinted by the status, else by the tint
                        stroke = null
                        fill = n.statusColor?.opacify(0.35) ?: tint?.opacify(0.35) ?: color.opacify(0.08)
                        rectangle(n.position.x, n.position.y, n.width, TITLE_HEIGHT)
                    }
                }
                val texts = mutableListOf<PanelText.Placed>()
                val labels = mutableListOf<PanelText.Placed>()
                drawer.circles {
                    stroke = null
                    for (n in layer) {
                        texts += text.placeInBox(computedStyle, text.ellipsized(computedStyle, n.title, n.width - 16.0),
                            Rectangle(n.position.x + 8.0, n.position.y, n.width - 16.0, TITLE_HEIGHT), 0.0, 0.5, color)
                        fun port(port: Port, index: Int, p: Vector2, connected: Boolean, isOutput: Boolean) {
                            val c = portColor(port)
                            fill = if (connected) c ?: accent else c?.opacify(0.45) ?: color.opacify(0.35)
                            circle(p, 5.0)
                            // the whole row when there is no port at the other side of it
                            val row = (0 until index).count { if (isOutput) n.isOutputShown(it) else n.isInputShown(it) }
                            val alone = row >= n.shownPorts(!isOutput)
                            val half = if (alone) n.width - 20.0 else n.width / 2.0 - 12.0
                            val box = if (isOutput) Rectangle(p.x - 10.0 - half, p.y - PORT_SPACING / 2.0, half, PORT_SPACING)
                            else Rectangle(p.x + 10.0, p.y - PORT_SPACING / 2.0, half, PORT_SPACING)
                            val name = text.ellipsized(computedStyle, port.name, half)
                            texts += text.placeInBox(computedStyle, name, box, if (isOutput) 1.0 else 0.0, 0.5, portText)
                            // the label after the name, which gives way to the name
                            val label = portLabel(n, port, isOutput) ?: return
                            val nameWidth = text.width(computedStyle, name)
                            val rest = half - nameWidth - 6.0
                            val shown = if (rest > 8.0) text.ellipsized(computedStyle, label, rest) else return
                            val shownWidth = text.width(computedStyle, shown)
                            val x = if (isOutput) box.x + box.width - nameWidth - 6.0 - shownWidth else box.x + nameWidth + 6.0
                            labels += text.placeInBox(computedStyle, shown, Rectangle(x, box.y, shownWidth + 1.0, box.height), 0.0, 0.5, labelText)
                        }
                        for ((i, p) in n.inputs.withIndex()) if (n.isInputShown(i)) port(p, i, n.inputPosition(i), (n to i) in connectedInputs, false)
                        for ((i, p) in n.outputs.withIndex()) if (n.isOutputShown(i)) port(p, i, n.outputPosition(i), (n to i) in connectedOutputs, true)
                        // the status under it all
                        n.status?.takeIf { n.hasStatus }?.let { status ->
                            val box = Rectangle(n.position.x + 8.0, n.position.y + n.height - STATUS_HEIGHT, n.width - 16.0, STATUS_HEIGHT - 2.0)
                            texts += text.placeInBox(computedStyle, text.ellipsized(computedStyle, status, box.width), box, 0.0, 0.5,
                                n.statusColor ?: color.opacify(0.6))
                        }
                    }
                }
                text.drawBatch(drawer, this@NodeEditor, "texts $l", texts)
                text.drawBatch(drawer, this@NodeEditor, "labels $l", labels)
                // the bodies that the host draws
                for (n in layer) n.body?.takeIf { n.hasBody }?.let { body ->
                    val box = n.bodyBounds
                    if (box.height > 0.0) drawer.isolated { body(drawer, box) }
                }
            }
            // the box that selects
            (gesture as? Gesture.Select)?.let { s ->
                drawer.fill = accent.opacify(0.12)
                drawer.stroke = accent
                drawer.strokeWeight = 1.0 / zoom
                drawer.rectangle(Rectangle(minOf(s.start.x, s.end.x), minOf(s.start.y, s.end.y), kotlin.math.abs(s.end.x - s.start.x), kotlin.math.abs(s.end.y - s.start.y)))
            }
        }
        // the badges over the nodes, at the same size at every zoom
        val placed = nodes.flatMap { badgeAreas(it) }
        if (placed.isNotEmpty()) drawer.isolated {
            drawer.stroke = ColorRGBa.fromHex("#2a2e36")
            drawer.strokeWeight = 1.5
            val labels = mutableListOf<PanelText.Placed>()
            for ((badge, area) in placed) {
                drawer.fill = badge.color
                drawer.box(area, BADGE_SIZE / 2.0)
                badge.label?.let { label ->
                    val c = badge.color
                    val light = 0.2126 * c.r + 0.7152 * c.g + 0.0722 * c.b > 0.5
                    labels += text.placeInBox(badgeStyle, label, area, 0.5, 0.5, if (light) ColorRGBa.BLACK.opacify(0.85) else ColorRGBa.WHITE)
                }
            }
            text.drawBatch(drawer, this@NodeEditor, "badges", labels)
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

/** a [NodeEditor], [init] adds its nodes and wires */
fun Element.nodeEditor(vararg classes: String, init: NodeEditor.() -> Unit): NodeEditor = initElement(classes, NodeEditor(), init)
