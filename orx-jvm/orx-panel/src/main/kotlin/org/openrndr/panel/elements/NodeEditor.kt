package org.openrndr.panel.elements

import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_DELETE
import org.openrndr.KeyModifier
import org.openrndr.MouseButton
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.Edit
import org.openrndr.panel.editHistory
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour

/** a port of a [GraphNode], an input or an output, of [type], which only connects to ports of the same type, or any */
class Port(val name: String, val type: String? = null)

/** a node of a [NodeEditor]: its [title], its [inputs] at the left and [outputs] at the right, at [position] */
class GraphNode(
    val title: String,
    val inputs: List<Port> = emptyList(),
    val outputs: List<Port> = emptyList(),
    var position: Vector2 = Vector2.ZERO,
    /** whatever the node stands for */
    val data: Any? = null
) {
    /** the width of the node in the coordinates of the graph */
    var width = 150.0

    val height: Double get() = NodeEditor.TITLE_HEIGHT + maxOf(inputs.size, outputs.size) * NodeEditor.PORT_SPACING + 8.0
    val bounds: Rectangle get() = Rectangle(position, width, height)

    /** the position of the input [i] in the coordinates of the graph */
    fun inputPosition(i: Int) = position + Vector2(0.0, NodeEditor.TITLE_HEIGHT + (i + 0.5) * NodeEditor.PORT_SPACING + 4.0)
    /** the position of the output [i] in the coordinates of the graph */
    fun outputPosition(i: Int) = position + Vector2(width, NodeEditor.TITLE_HEIGHT + (i + 0.5) * NodeEditor.PORT_SPACING + 4.0)
}

/** a wire from the output [output] of [from] to the input [input] of [to] */
data class Wire(val from: GraphNode, val output: Int, val to: GraphNode, val input: Int)

/**
 * A graph of [nodes] with ports, connected by [wires]. Dragging a node moves it with the other selected nodes, dragging
 * from an output draws a wire that connects to the input it is released on, dragging a wire off an input detaches it;
 * an input has one wire at most. A click selects a node, with shift more; dragging on the background selects the nodes
 * in the box, and delete removes the selected nodes. The wheel zooms around the pointer, dragging with the middle
 * button or with alt pans. The changes can be undone, see [org.openrndr.panel.History]. Nodes are drawn in the colors
 * of the style of the editor, the selected ones and wires in that of [accent].
 */
class NodeEditor : Element(ElementType("node-editor")) {
    override val handlesKeyboardFocus = true
    override val handlesDoubleClick = true

    companion object {
        const val TITLE_HEIGHT = 26.0
        const val PORT_SPACING = 22.0
    }

    class Events : AutoCloseable {
        /** a wire was connected */
        val connected = Event<Wire>("node-editor-connected")

        /** a wire was disconnected */
        val disconnected = Event<Wire>("node-editor-disconnected")

        /** nodes were removed */
        val removed = Event<List<GraphNode>>("node-editor-removed")

        /** the selected nodes changed */
        val selected = Event<Set<GraphNode>>("node-editor-selected")

        /** the background was double clicked, at the position in the coordinates of the graph, like to add a node */
        val requested = Event<Vector2>("node-editor-requested")

        override fun close() {
            connected.close(); disconnected.close(); removed.close(); selected.close(); requested.close()
        }
    }

    val events = Events()

    val nodes = mutableListOf<GraphNode>()
    val wires = mutableListOf<Wire>()

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

    /** true when a wire can go from [output] to [input], by default when their types are the same or either has none */
    var canConnect: (output: Port, input: Port) -> Boolean = { o, i -> o.type == null || i.type == null || o.type == i.type }

    /** adds [node] at [position] in the coordinates of the graph */
    fun add(node: GraphNode, position: Vector2 = node.position): GraphNode {
        node.position = position
        nodes.add(node)
        requestRedraw()
        return node
    }

    /** connects the output [output] of [from] to the input [input] of [to], replacing the wire at that input */
    fun connect(from: GraphNode, output: Int, to: GraphNode, input: Int): Wire? {
        if (from === to || !canConnect(from.outputs[output], to.inputs[input])) return null
        val wire = Wire(from, output, to, input)
        if (wire in wires) return wire
        wires.filter { it.to === to && it.input == input }.forEach { disconnect(it) }
        wires.add(wire)
        requestRedraw()
        events.connected.trigger(wire)
        return wire
    }

    /** removes [wire] */
    fun disconnect(wire: Wire) {
        if (wires.remove(wire)) {
            requestRedraw()
            events.disconnected.trigger(wire)
        }
    }

    /** removes [removed] and their wires */
    fun remove(removed: Collection<GraphNode>) {
        val set = removed.toSet()
        wires.filter { it.from in set || it.to in set }.forEach { disconnect(it) }
        nodes.removeAll(set)
        selection = selection - set
        requestRedraw()
        events.removed.trigger(set.toList())
    }

    //<editor-fold desc="coordinates">
    /** [p], a point of the window, in the coordinates of the graph */
    fun graphPosition(p: Vector2): Vector2 = (p - Vector2(layout.screenX, layout.screenY) - pan) / zoom

    private fun local(g: Vector2) = g * zoom + pan

    private fun nodeAt(g: Vector2) = nodes.lastOrNull { g in it.bounds }

    private fun outputAt(g: Vector2): Pair<GraphNode, Int>? = nodes.asReversed().firstNotNullOfOrNull { n ->
        n.outputs.indices.firstOrNull { n.outputPosition(it).distanceTo(g) < 9.0 }?.let { n to it }
    }

    private fun inputAt(g: Vector2): Pair<GraphNode, Int>? = nodes.asReversed().firstNotNullOfOrNull { n ->
        n.inputs.indices.firstOrNull { n.inputPosition(it).distanceTo(g) < 9.0 }?.let { n to it }
    }
    //</editor-fold>

    //<editor-fold desc="the pointer">
    private sealed class Gesture {
        class Move(val start: Vector2, val from: Map<GraphNode, Vector2>) : Gesture()
        class Connect(val from: GraphNode, val output: Int, var end: Vector2, val detached: Wire?) : Gesture()
        class Select(val start: Vector2, var end: Vector2, val before: Set<GraphNode>) : Gesture()
        class Pan(val start: Vector2, val from: Vector2) : Gesture()
    }

    private var gesture: Gesture? = null

    /** the wire that is drawn while it is dragged, in the coordinates of the graph */
    private val dragging get() = gesture as? Gesture.Connect

    init {
        mouse.pressed.listen {
            val g = graphPosition(it.position)
            val shift = KeyModifier.SHIFT in it.modifiers
            gesture = when {
                it.button == MouseButton.CENTER || KeyModifier.ALT in it.modifiers -> Gesture.Pan(it.position, pan)
                outputAt(g) != null -> outputAt(g)!!.let { (n, o) -> Gesture.Connect(n, o, g, null) }
                inputAt(g)?.let { (n, i) -> wires.firstOrNull { w -> w.to === n && w.input == i } } != null -> {
                    // the wire is taken off the input and follows the pointer
                    val (n, i) = inputAt(g)!!
                    val wire = wires.first { w -> w.to === n && w.input == i }
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
                    nodes.remove(node); nodes.add(node)
                    Gesture.Move(g, selection.associateWith { n -> n.position })
                }
                else -> {
                    if (!shift) selection = emptySet()
                    Gesture.Select(g, g, selection)
                }
            }
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            val g = graphPosition(it.position)
            when (val current = gesture) {
                is Gesture.Pan -> pan = current.from + (it.position - current.start)
                is Gesture.Move -> {
                    val d = g - current.start
                    for ((n, p) in current.from) n.position = p + d
                    requestRedraw()
                }
                is Gesture.Connect -> { current.end = g; requestRedraw() }
                is Gesture.Select -> {
                    current.end = g
                    val box = Rectangle.fromCenter((current.start + g) / 2.0, kotlin.math.abs(g.x - current.start.x), kotlin.math.abs(g.y - current.start.y))
                    selection = current.before + nodes.filter { n -> n.bounds.intersects(box) }
                }
                null -> {}
            }
            it.cancelPropagation()
        }
        mouse.clicked.listen { it.cancelPropagation() }
        mouse.released.listen { finish(graphPosition(it.position)) }
        mouse.doubleClicked.listen {
            val g = graphPosition(it.position)
            if (nodeAt(g) == null) events.requested.trigger(g)
            it.cancelPropagation()
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
        when (val current = gesture) {
            is Gesture.Move -> {
                val to = current.from.keys.associateWith { it.position }
                if (to.any { (n, p) -> p != current.from[n] }) {
                    editHistory?.record(Edit("Move nodes", null,
                        { for ((n, p) in current.from) n.position = p; requestRedraw() },
                        { for ((n, p) in to) n.position = p; requestRedraw() }))
                }
            }
            is Gesture.Connect -> {
                val target = inputAt(g)
                val replaced = target?.let { (n, i) -> wires.firstOrNull { w -> w.to === n && w.input == i } }
                val wire = target?.let { (n, i) -> connect(current.from, current.output, n, i) }
                val detached = current.detached
                if (wire != detached) {
                    editHistory?.record(Edit(if (wire != null) "Connect" else "Disconnect", null, {
                        wire?.let { disconnect(it) }
                        replaced?.let { wires.add(it); events.connected.trigger(it) }
                        detached?.let { wires.add(it); events.connected.trigger(it) }
                        requestRedraw()
                    }, {
                        detached?.let { disconnect(it) }
                        wire?.let { connect(it.from, it.output, it.to, it.input) }
                    }))
                }
                requestRedraw()
            }
            else -> {}
        }
        gesture = null
    }

    private fun removeWithUndo(removed: Set<GraphNode>) {
        val lostWires = wires.filter { it.from in removed || it.to in removed }
        val order = nodes.toList()
        remove(removed)
        editHistory?.record(Edit("Remove nodes", null, {
            nodes.clear(); nodes.addAll(order)
            for (w in lostWires) { wires.add(w); events.connected.trigger(w) }
            selection = removed
            requestRedraw()
        }, { remove(removed) }))
    }
    //</editor-fold>

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        drawer.isolated {
            // a grid that moves and scales with the graph
            val step = 24.0 * zoom
            if (step > 6.0) {
                drawer.stroke = null
                drawer.fill = color.opacify(0.12)
                val w = layout.screenWidth
                val h = layout.screenHeight
                val ox = Math.floorMod(pan.x.toInt(), step.toInt().coerceAtLeast(1)).toDouble()
                val oy = Math.floorMod(pan.y.toInt(), step.toInt().coerceAtLeast(1)).toDouble()
                val points = mutableListOf<Vector2>()
                var y = oy
                while (y < h) { var x = ox; while (x < w) { points.add(Vector2(x, y)); x += step }; y += step }
                drawer.circles(points, 1.0)
            }
            drawer.translate(pan)
            drawer.scale(zoom)
            // the wires under the nodes
            drawer.fill = null
            drawer.lineCap = LineCap.ROUND
            drawer.strokeWeight = 2.0
            fun wire(a: Vector2, b: Vector2, c: ColorRGBa) {
                val d = maxOf(40.0, kotlin.math.abs(b.x - a.x) * 0.5)
                drawer.stroke = c
                drawer.contour(contour { moveTo(a); curveTo(a + Vector2(d, 0.0), b - Vector2(d, 0.0), b) })
            }
            for (w in wires) wire(w.from.outputPosition(w.output), w.to.inputPosition(w.input), accent.opacify(0.8))
            dragging?.let { wire(it.from.outputPosition(it.output), inputAt(it.end)?.let { (n, i) -> n.inputPosition(i) } ?: it.end, accent) }
            // the nodes
            for (n in nodes) {
                val selected = n in selection
                drawer.stroke = if (selected) accent else color.opacify(0.25)
                drawer.strokeWeight = if (selected) 2.0 else 1.0
                drawer.fill = ColorRGBa.fromHex("#2a2e36")
                drawer.rectangle(n.bounds)
                drawer.stroke = null
                drawer.fill = color.opacify(0.08)
                drawer.rectangle(n.position.x, n.position.y, n.width, TITLE_HEIGHT)
                text.drawInBox(drawer, this@NodeEditor, "title ${n.title}", computedStyle, text.ellipsized(computedStyle, n.title, n.width - 16.0),
                    Rectangle(n.position.x + 8.0, n.position.y, n.width - 16.0, TITLE_HEIGHT), 0.0, 0.5, color)
                for ((i, port) in n.inputs.withIndex()) {
                    val p = n.inputPosition(i)
                    val connected = wires.any { it.to === n && it.input == i }
                    drawer.fill = if (connected) accent else color.opacify(0.35)
                    drawer.circle(p, 5.0)
                    text.drawInBox(drawer, this@NodeEditor, "in ${port.name}", computedStyle, port.name,
                        Rectangle(p.x + 10.0, p.y - PORT_SPACING / 2.0, n.width / 2.0, PORT_SPACING), 0.0, 0.5, color.opacify(0.75))
                }
                for ((i, port) in n.outputs.withIndex()) {
                    val p = n.outputPosition(i)
                    val connected = wires.any { it.from === n && it.output == i }
                    drawer.fill = if (connected) accent else color.opacify(0.35)
                    drawer.circle(p, 5.0)
                    text.drawInBox(drawer, this@NodeEditor, "out ${port.name}", computedStyle, port.name,
                        Rectangle(p.x - 10.0 - n.width / 2.0, p.y - PORT_SPACING / 2.0, n.width / 2.0, PORT_SPACING), 1.0, 0.5, color.opacify(0.75))
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
    }

    override fun close() {
        super.close()
        events.close()
    }
}

/** a [NodeEditor], [init] adds its nodes and wires */
fun Element.nodeEditor(vararg classes: String, init: NodeEditor.() -> Unit): NodeEditor = initElement(classes, NodeEditor(), init)
