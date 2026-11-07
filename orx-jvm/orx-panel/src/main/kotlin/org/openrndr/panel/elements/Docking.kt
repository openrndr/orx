package org.openrndr.panel.elements

import org.openrndr.ApplicationWindow
import org.openrndr.Program
import org.openrndr.WindowConfiguration
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.document.body
import org.openrndr.panel.document.document
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.window

/** where a panel is docked relative to a group of a [Dock]: in it as a tab, or next to it on a side */
enum class DockZone { CENTER, LEFT, RIGHT, TOP, BOTTOM }

/** a panel of a [Dock]: its [title] on its tab and its [content] */
class DockPanel internal constructor(val title: String) {
    val content: Div = Div(ElementType("dock-content"))

    /** the window the panel is in when it is taken out of the dock, null when it is docked */
    var window: ApplicationWindow? = null
        internal set

    /**
     * Called in the program of the window the panel is taken out into, before the panel is added to it, so what it
     * extends the program with is drawn under the panel; like a drawing that shows through a transparent panel.
     */
    var inWindow: (Program.() -> Unit)? = null
}

/** the layout of a [Dock]: groups of panels shown as tabs, and splits of two nodes */
sealed class DockNode {
    class Group(val panels: MutableList<DockPanel> = mutableListOf(), var selected: Int = 0) : DockNode() {
        val selectedPanel: DockPanel? get() = panels.getOrNull(selected.coerceIn(0, maxOf(0, panels.lastIndex)))
    }

    class Split(val vertical: Boolean, var first: DockNode, var second: DockNode) : DockNode() {
        /** the size of the first node in pixels, negative for half of the split */
        var position = -1.0
    }

    /** the groups in the node, first to last */
    fun groups(): List<Group> = when (this) {
        is Group -> listOf(this)
        is Split -> first.groups() + second.groups()
    }
}

/** the drag of a panel of a [Dock] by its tab */
data class PanelDrag(val dock: Dock, val panel: DockPanel)

/**
 * Panels in groups, of which each shows one panel at a time under the tabs of all, and in splits of groups next to and
 * above each other. A panel is added with [panel]; dragging its tab onto another group docks it there: in the middle as
 * a tab, at a side next to the group. A panel dragged out of the window, or of which the tab is double clicked, goes
 * into a window of its own, and back into the dock when that window is closed.
 *
 * The groups are `dock-group`s with a `dock-tab-bar` of `dock-tab`s, the tab of the panel that shows is `:selected`;
 * the content of a panel is a `dock-content`.
 */
class Dock : Div(ElementType("dock")) {
    /** the layout, null when the dock has no panels */
    var root: DockNode? = null
        private set

    /** the panels that are taken out into windows of their own */
    val floating = mutableListOf<DockPanel>()

    /** the layout changed */
    val changed = Event<Dock>("dock-changed")

    private var dirty = true
    private val splitPanes = mutableMapOf<DockNode.Split, SplitPane>()
    private val returning = java.util.concurrent.ConcurrentLinkedQueue<DockPanel>()

    /** all panels, docked or in windows of their own */
    val panels: List<DockPanel> get() = (root?.groups()?.flatMap { it.panels } ?: emptyList()) + floating

    /** adds a panel with [title], built by [init], in [zone] of the group [next] to, the last group when null */
    fun panel(title: String, zone: DockZone = DockZone.CENTER, next: DockPanel? = null, init: Div.() -> Unit): DockPanel {
        val panel = DockPanel(title)
        panel.content.init()
        place(panel, next?.let { groupOf(it) } ?: root?.groups()?.lastOrNull(), zone)
        return panel
    }

    /** the group that [panel] is docked in, null when it is not docked */
    fun groupOf(panel: DockPanel): DockNode.Group? = root?.groups()?.firstOrNull { panel in it.panels }

    /** docks [panel] in [zone] of [group], the root when null, out of where it is */
    fun dock(panel: DockPanel, group: DockNode.Group?, zone: DockZone) {
        val source = groupOf(panel)
        if (source != null && source === group && (zone == DockZone.CENTER || source.panels.size == 1)) return
        undock(panel)
        place(panel, group?.takeIf { g -> root?.groups()?.any { it === g } == true }, zone)
    }

    /** shows [panel], selecting its tab */
    fun select(panel: DockPanel) {
        val group = groupOf(panel) ?: return
        group.selected = group.panels.indexOf(panel)
        rebuild()
    }

    private fun place(panel: DockPanel, group: DockNode.Group?, zone: DockZone) {
        val target = group ?: root?.groups()?.lastOrNull()
        when {
            target == null -> root = DockNode.Group(mutableListOf(panel))
            zone == DockZone.CENTER -> {
                target.panels.add(panel)
                target.selected = target.panels.lastIndex
            }
            else -> {
                val added = DockNode.Group(mutableListOf(panel))
                val vertical = zone == DockZone.TOP || zone == DockZone.BOTTOM
                val before = zone == DockZone.LEFT || zone == DockZone.TOP
                replace(target, DockNode.Split(vertical, if (before) added else target, if (before) target else added))
            }
        }
        rebuild()
    }

    /** takes [panel] out of its group, and the group out of the layout when it is empty */
    private fun undock(panel: DockPanel) {
        val group = groupOf(panel) ?: return
        val index = group.panels.indexOf(panel)
        group.panels.removeAt(index)
        if (group.selected >= index) group.selected = maxOf(0, group.selected - 1)
        if (group.panels.isEmpty()) {
            val parent = parentOf(group)
            if (parent == null) root = null
            else replace(parent, if (parent.first === group) parent.second else parent.first)
        }
        rebuild()
    }

    private fun parentOf(node: DockNode, from: DockNode? = root): DockNode.Split? = when (from) {
        is DockNode.Split -> if (from.first === node || from.second === node) from else parentOf(node, from.first) ?: parentOf(node, from.second)
        else -> null
    }

    private fun replace(node: DockNode, with: DockNode) {
        val parent = parentOf(node)
        when {
            parent == null -> root = with
            parent.first === node -> parent.first = with
            else -> parent.second = with
        }
    }

    //<editor-fold desc="windows of their own">
    /** takes [panel] out of the dock into a window of its own, at [position] on the screen, next to the window when null */
    fun float(panel: DockPanel, position: IntVector2? = null) {
        val cm = (root() as? Body)?.controlManager ?: return
        if (panel.window != null) return
        val program = cm.program
        val size = (groupOf(panel)?.let { group -> group.elementSize() } ?: Vector2(320.0, 240.0))
        undock(panel)
        panel.content.parent?.remove(panel.content)
        floating.add(panel)
        val styles = cm.document?.styleSheets?.toList() ?: defaultStyles()
        panel.window = try {
            program.window(
                WindowConfiguration(
                    title = panel.title, width = size.x.toInt().coerceAtLeast(200), height = size.y.toInt().coerceAtLeast(120),
                    position = position ?: (program.window.position.toInt() + IntVector2(program.width + 12, 0)),
                    resizable = true, utilityWindow = true
                )
            ) {
                // the background of the window, under what the panel draws there and the panel
                extend { drawer.clear(windowBackground) }
                panel.inWindow?.invoke(this)
                val windowManager = ControlManager()
                windowManager.document = document {
                    styleSheets.addAll(styles + styleSheets(floatingCss))
                    body {
                        div("dock-window") { append(panel.content) }
                    }
                }
                panel.content.invalidateStyle()
                extend(windowManager)
                // the panel goes back into the dock when the window closes, in the frame of the dock
                this.window.closed.listen {
                    if (panel.window != null) {
                        panel.content.parent?.remove(panel.content)
                        returning.add(panel)
                        program.window.requestDraw()
                    }
                }
            }
        } catch (e: Throwable) {
            null
        }
        if (panel.window == null) {
            floating.remove(panel)
            place(panel, null, DockZone.CENTER)
        }
        changed.trigger(this)
    }

    private fun DockNode.Group.elementSize(): Vector2? =
        findGroupElement(this@Dock, this)?.let { Vector2(it.layout.screenWidth, it.layout.screenHeight) }

    private fun findGroupElement(e: Element, group: DockNode.Group): DockGroup? =
        if (e is DockGroup && e.group === group) e else e.children.firstNotNullOfOrNull { findGroupElement(it, group) }

    /** puts [panel] back into the dock from its window, which closes */
    fun redock(panel: DockPanel) {
        val w = panel.window ?: return
        panel.window = null
        panel.content.parent?.remove(panel.content)
        floating.remove(panel)
        place(panel, null, DockZone.CENTER)
        w.close()
    }

    /** the color of the windows that panels are taken out into, under the panels */
    var windowBackground: ColorRGBa = ColorRGBa.fromHex("#1d1f23")

    private val floatingCss = """
        .dock-window { width: 100%; height: 100% }
        .dock-window > dock-content { width: 100%; height: 100% }
    """
    //</editor-fold>

    override fun prepare() {
        while (true) {
            val panel = returning.poll() ?: break
            if (panel !in floating) continue
            panel.window = null
            floating.remove(panel)
            place(panel, null, DockZone.CENTER)
        }
        // splits without a position start at half of their size, the outer ones first, since the sizes of the
        // inner ones are known once those are placed
        fun place(node: DockNode?) {
            if (node !is DockNode.Split) return
            val pane = splitPanes[node] ?: return
            if (node.position < 0.0) {
                val size = if (node.vertical) pane.layout.screenHeight else pane.layout.screenWidth
                if (size > 0.0) {
                    node.position = size / 2.0
                    pane.position = node.position
                }
            } else {
                place(node.first)
                place(node.second)
            }
        }
        place(root)
        if (dirty) build()
    }

    /** the elements of the layout are built again before the next frame */
    private fun rebuild() {
        dirty = true
        changed.trigger(this)
        requestLayout()
    }

    private fun build() {
        dirty = false
        // the positions of the splitters as they were dragged
        for ((split, pane) in splitPanes) if (split.position >= 0.0) split.position = pane.position
        splitPanes.clear()
        // the contents of the panels are kept, the elements around them are new
        val contents = panels.map { it.content }.toSet()
        fun detach(e: Element) {
            for (child in e.children.toList()) {
                if (child !in contents) detach(child)
                // a content in a window of its own is only taken out of the list
                else if (child.parent === e) e.remove(child) else e.children.remove(child)
            }
        }
        detach(this)
        for (child in children.toList()) {
            remove(child)
            child.close()
        }
        root?.let { append(element(it)) }
        invalidateStyle()
        requestLayout()
        requestRedraw()
    }

    private fun element(node: DockNode): Element = when (node) {
        is DockNode.Group -> DockGroup(this, node)
        is DockNode.Split -> SplitPane(node.vertical).also { pane ->
            pane.first.append(element(node.first))
            pane.second.append(element(node.second))
            if (node.position >= 0.0) pane.position = node.position
            splitPanes[node] = pane
        }
    }

    internal fun dropped(drag: PanelDrag, group: DockNode.Group, zone: DockZone) {
        if (drag.dock === this) dock(drag.panel, group, zone)
    }
}

/** a group of a [Dock]: the tabs of its panels and the content of the one that shows */
class DockGroup internal constructor(val dock: Dock, val group: DockNode.Group) : Div(ElementType("dock-group")) {
    /** the zone that a dragged panel is over, null when none is */
    var zone: DockZone? = null
        private set(value) {
            if (value != field) {
                field = value
                showZone(value)
            }
        }

    init {
        val bar = Div(ElementType("dock-tab-bar"))
        append(bar)
        for ((i, panel) in group.panels.withIndex()) bar.append(DockTab(dock, panel, i == group.selected))
        group.selectedPanel?.let { append(it.content) }
        dnd.accepts = { (it as? PanelDrag)?.dock === dock }
        dnd.over.listen { zone = zoneAt(it.position) }
        dnd.left.listen { zone = null }
        dnd.dropped.listen {
            val z = zoneAt(it.position)
            zone = null
            dock.dropped(it.data as PanelDrag, group, z)
        }
    }

    /** the zone at [p], a point of the window: the sides are the outer quarters of the group */
    fun zoneAt(p: Vector2): DockZone {
        val w = layout.screenWidth
        val h = layout.screenHeight
        val u = (p.x - layout.screenX) / w
        val v = (p.y - layout.screenY) / h
        val edges = listOf(DockZone.LEFT to u, DockZone.RIGHT to 1.0 - u, DockZone.TOP to v, DockZone.BOTTOM to 1.0 - v)
        val (closest, distance) = edges.minBy { it.second }
        return if (distance < 0.25) closest else DockZone.CENTER
    }

    private var overlay: Div? = null

    // the zone that a panel would be docked in is shown over the content, a `dock-zone`
    private fun showZone(z: DockZone?) {
        overlay?.let { remove(it); it.close() }
        overlay = null
        if (z == null) return
        val w = layout.screenWidth
        val h = layout.screenHeight
        val r = when (z) {
            DockZone.CENTER -> Rectangle(0.0, 0.0, w, h)
            DockZone.LEFT -> Rectangle(0.0, 0.0, w / 2.0, h)
            DockZone.RIGHT -> Rectangle(w / 2.0, 0.0, w / 2.0, h)
            DockZone.TOP -> Rectangle(0.0, 0.0, w, h / 2.0)
            DockZone.BOTTOM -> Rectangle(0.0, h / 2.0, w, h / 2.0)
        }.offsetEdges(-3.0)
        overlay = Div(ElementType("dock-zone")).apply {
            style {
                this.position = Position.ABSOLUTE
                this.left = r.x.px
                this.top = r.y.px
                this.width = r.width.px
                this.height = r.height.px
            }
        }.also { append(it) }
        requestLayout()
    }
}

/** the tab of a panel of a [Dock]: a click shows it, dragging docks it elsewhere, a double click floats it */
class DockTab internal constructor(val dock: Dock, val panel: DockPanel, selected: Boolean) : TextElement(ElementType("dock-tab")) {
    override val handlesDoubleClick = true

    init {
        text(panel.title)
        if (selected) pseudoClasses.add(ElementPseudoClass("selected"))
        dnd.data = { PanelDrag(dock, panel) }
        dnd.label = { panel.title }
        mouse.pressed.listen { it.cancelPropagation() }
        mouse.clicked.listen {
            if ((root() as? Body)?.controlManager?.draggingAndDropping != true) dock.select(panel)
            it.cancelPropagation()
        }
        mouse.doubleClicked.listen {
            dock.float(panel)
            it.cancelPropagation()
        }
        // a drag that ends outside the window takes the panel out into a window of its own, there
        dnd.ended.listen {
            val cm = (root() as? Body)?.controlManager ?: return@listen
            val p = it.position
            val program = cm.program
            if (p.x < 0.0 || p.y < 0.0 || p.x > program.width || p.y > program.height) {
                dock.float(panel, program.window.position.toInt() + p.toInt())
            }
        }
    }
}

/** a [Dock], [init] adds its panels with [Dock.panel] */
fun Element.dock(vararg classes: String, init: Dock.() -> Unit): Dock = initElement(classes, Dock(), init)
