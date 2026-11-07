package org.openrndr.panel.elements

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_ENTER
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.panel.style.*
import org.openrndr.shape.contour

/**
 * A tree of items that expand and collapse, like a scene graph or a file system: [roots] are the items at the top,
 * [childrenOf] gives the children of an item, [label] its text. Only the rows in view are built, see [VirtualList], so
 * large trees are as quick as small ones.
 *
 * A click selects an item, a click on its arrow or a double click expands or collapses it. With the keyboard focus, up
 * and down select the item above and below, right expands an item or moves to its first child, left collapses it or
 * moves to its parent, enter activates it. The rows are `tree-row` elements, `:selected` when selected, with the
 * attribute `data-depth`.
 */
class TreeView<T>(
    roots: List<T>,
    val childrenOf: (T) -> List<T>,
    val label: (T) -> String,
    val rowHeight: Double = 24.0
) : Div(ElementType("tree-view")) {
    /** an item in view: the item and its depth */
    private class Row<T>(val item: T, val depth: Int)

    /** the events of the tree */
    class Events<T> : AutoCloseable {
        /** an item is selected */
        val selected = Event<T>("tree-selected")

        /** an item is activated, by enter or a double click on a leaf */
        val activated = Event<T>("tree-activated")

        /** an item is expanded or collapsed, with its new state */
        val toggled = Event<Pair<T, Boolean>>("tree-toggled")

        /** an item is dragged onto another item, the second, when the tree is [TreeView.draggable] */
        val dropped = Event<Pair<T, T>>("tree-dropped")

        override fun close() {
            dropped.close()
            selected.close()
            activated.close()
            toggled.close()
        }
    }

    val events = Events<T>()

    override val handlesKeyboardFocus = true

    /** the items at the top, a change shows the tree again */
    var roots: List<T> = roots
        set(value) {
            field = value
            rebuild()
        }

    /** true when items can be dragged onto other items, see [Events.dropped]; the tree does not change itself */
    var draggable = false

    /** a dragged item of [tree] */
    data class ItemDrag(val tree: TreeView<*>, val item: Any?)

    /** true when [item] is [ancestor] or in it */
    internal fun isIn(item: T, ancestor: T): Boolean = item == ancestor || childrenOf(ancestor).any { isIn(item, it) }

    /** the items that are expanded, a change shows the tree again */
    val expanded = org.openrndr.panel.collections.ObservableHashSet<T>()

    /** the item that is selected, null when none is */
    var selection: T? = null
        set(value) {
            if (value != field) {
                field = value
                list.rows().forEach { it.updateSelected() }
                value?.let { events.selected.trigger(it) }
            }
        }

    private var rows: List<Row<T>> = emptyList()

    private val list: VirtualList<Row<T>> = VirtualList(emptyList(), rowHeight) { row, _ ->
        append(TreeRow(this@TreeView, row.item, row.depth))
    }

    init {
        append(list)
        rebuild()
        expanded.changed.listen { rebuild() }
        keyboard.pressed.listen {
            val index = rows.indexOfFirst { r -> r.item == selection }
            when (it.key) {
                KEY_ARROW_DOWN -> select(rows.getOrNull(index + 1) ?: return@listen)
                KEY_ARROW_UP -> select(rows.getOrNull((index - 1).coerceAtLeast(0)) ?: return@listen)
                KEY_ARROW_RIGHT -> {
                    val row = rows.getOrNull(index) ?: return@listen
                    if (childrenOf(row.item).isEmpty()) return@listen
                    if (row.item in expanded) rows.getOrNull(index + 1)?.let(::select) else toggle(row.item)
                }
                KEY_ARROW_LEFT -> {
                    val row = rows.getOrNull(index) ?: return@listen
                    if (row.item in expanded) toggle(row.item)
                    else rows.subList(0, index).lastOrNull { r -> r.depth < row.depth }?.let(::select)
                }
                KEY_ENTER -> selection?.let(events.activated::trigger)
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }

    private fun select(row: Row<T>) {
        selection = row.item
        // the selected row is scrolled into view
        val index = rows.indexOf(row)
        val top = index * rowHeight
        val height = list.layout.screenHeight
        if (top < list.scrollTop) list.scrollTo(top = top, smooth = false)
        else if (top + rowHeight > list.scrollTop + height) list.scrollTo(top = top + rowHeight - height, smooth = false)
    }

    /** expands [item] when it is collapsed, collapses it otherwise */
    fun toggle(item: T) {
        val open = item !in expanded
        if (open) expanded.add(item) else expanded.remove(item)
        events.toggled.trigger(item to open)
    }

    /** the items in view, from the top, with their depths */
    private fun rebuild() {
        val result = mutableListOf<Row<T>>()
        fun visit(item: T, depth: Int) {
            result.add(Row(item, depth))
            if (item in expanded) childrenOf(item).forEach { visit(it, depth + 1) }
        }
        roots.forEach { visit(it, 0) }
        rows = result
        list.items = result
    }

    private fun VirtualList<Row<T>>.rows(): List<TreeRow<T>> = children.flatMap { it.children }.filterIsInstance<TreeRow<T>>()

    override fun close() {
        super.close()
        events.close()
    }
}

/** a row of a [TreeView]: the arrow of an item with children, and its label, indented by its depth */
class TreeRow<T> internal constructor(private val tree: TreeView<T>, val item: T, val depth: Int) : Div(ElementType("tree-row")) {
    override val handlesDoubleClick = true

    private val indent get() = 8.0 + depth * 16.0
    private val leaf get() = tree.childrenOf(item).isEmpty()

    init {
        setAttribute("data-depth", depth.toString())
        updateSelected()
        // items are dragged onto other items, not onto themselves or what is in them
        dnd.data = { if (tree.draggable) TreeView.ItemDrag(tree, item) else null }
        dnd.label = { tree.label(item) }
        @Suppress("UNCHECKED_CAST")
        dnd.accepts = { data -> tree.draggable && data is TreeView.ItemDrag && data.tree === tree && !tree.isIn(item, data.item as T) }
        @Suppress("UNCHECKED_CAST")
        dnd.dropped.listen { tree.events.dropped.trigger((it.data as TreeView.ItemDrag).item as T to item) }
        mouse.clicked.listen {
            val local = it.position.x - layout.screenX
            if (!leaf && local in indent - 4.0..indent + 14.0) tree.toggle(item) else tree.selection = item
            it.cancelPropagation()
        }
        mouse.doubleClicked.listen {
            if (leaf) tree.events.activated.trigger(item) else tree.toggle(item)
            it.cancelPropagation()
        }
    }

    internal fun updateSelected() {
        if (tree.selection == item) pseudoClasses.add(ElementPseudoClass("selected"))
        else pseudoClasses.remove(ElementPseudoClass("selected"))
    }

    override fun draw(drawer: Drawer) {
        super.draw(drawer)
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val h = layout.screenHeight
        if (!leaf) {
            // the arrow points right when collapsed, down when expanded
            val c = org.openrndr.math.Vector2(indent + 5.0, h / 2.0)
            val open = item in tree.expanded
            drawer.fill = color
            drawer.stroke = null
            drawer.contour(contour {
                if (open) { moveTo(c.x - 4.5, c.y - 2.5); lineTo(c.x + 4.5, c.y - 2.5); lineTo(c.x, c.y + 3.5) }
                else { moveTo(c.x - 2.5, c.y - 4.5); lineTo(c.x + 3.5, c.y); lineTo(c.x - 2.5, c.y + 4.5) }
                close()
            })
        }
        panelText?.drawInBox(drawer, this, "label", computedStyle, tree.label(item),
            org.openrndr.shape.Rectangle(indent + 16.0, 0.0, layout.screenWidth - indent - 20.0, h), 0.0, 0.5, color)
    }
}

/** a [TreeView] of [roots], whose children [childrenOf] gives, labeled by [label] */
fun <T> Element.treeView(
    roots: List<T>, childrenOf: (T) -> List<T>, label: (T) -> String = { it.toString() },
    vararg classes: String, rowHeight: Double = 24.0, init: TreeView<T>.() -> Unit = {}
): TreeView<T> = initElement(classes, TreeView(roots, childrenOf, label, rowHeight), init)
