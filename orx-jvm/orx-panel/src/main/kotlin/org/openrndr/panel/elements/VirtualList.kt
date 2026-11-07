package org.openrndr.panel.elements

import org.openrndr.math.Vector2
import org.openrndr.panel.animateMoveFrom
import org.openrndr.panel.Edit
import org.openrndr.panel.editHistory
import org.openrndr.panel.style.height
import org.openrndr.panel.style.left
import org.openrndr.panel.style.position
import org.openrndr.panel.style.top
import org.openrndr.panel.style.px
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A list of [items] that scrolls, with rows of [rowHeight] pixels that [row] builds; only the rows in view, and
 * [overscan] rows around them, exist as elements, so that long lists are as quick as short ones. Spacers above and below
 * the rows keep the height of the whole list, the list scrolls as if all rows were there. Style the list with a height
 * and the rows with the class `virtual-row`. It scrolls with the wheel and draws its background like a [Div].
 */
class VirtualList<T>(
    items: List<T>,
    val rowHeight: Double,
    private val row: Div.(item: T, index: Int) -> Unit
) : Div(ElementType("virtual-list")) {
    /** the items, a change builds the rows again */
    var items: List<T> = items
        set(value) {
            // the rows of items that stay slide from where they were to where they go
            if (animateMoves) {
                moved.clear()
                for ((i, row) in rows) field.getOrNull(i)?.let { item ->
                    if (row.layout.screenHeight > 0.0) moved.add(item to Vector2(row.layout.screenX, row.layout.screenY))
                }
            }
            field = value
            rows.values.forEach { it.close() }
            rows.clear()
            range = IntRange.EMPTY
            requestLayout()
        }

    /** true when the rows of items that stay slide to their new places when the items change, like when sorted */
    var animateMoves = true

    /** the items of the rows before the items changed, and where those rows were */
    private val moved = mutableListOf<Pair<T, Vector2>>()

    /** the rows that are built around the rows in view, above and below */
    var overscan = 4

    /**
     * True when the rows can be dragged to other places in the list, [reordered] tells where to; the list does not
     * change its items itself
     */
    var reorderable = false
        set(value) {
            field = value
            items = items
        }

    /** the text of the ghost of a dragged row, its item as text by default */
    var rowLabel: (T) -> String = { it.toString() }

    /** a row is dragged from the first index to the second, the index it has after it is moved */
    val reordered = org.openrndr.events.Event<Pair<Int, Int>>("virtual-list-reordered")

    /** a dragged row of [list] at [index] */
    data class RowDrag(val list: VirtualList<*>, val index: Int)

    /** the line that shows where a dragged row goes, null while no row is dragged over the list */
    private var marker: Div? = null

    /** the index a row dropped at [y], in the coordinates of the window, goes to */
    private fun insertion(y: Double) = ((y - layout.screenY + scrollTop) / rowHeight + 0.5).toInt().coerceIn(0, items.size)

    init {
        dnd.accepts = { reorderable && it is RowDrag && it.list === this }
        dnd.over.listen { event ->
            val at = insertion(event.position.y)
            val m = marker ?: Div().apply { classes.add(ElementClass("virtual-marker")) }.also { marker = it }
            m.style {
                this.position = org.openrndr.panel.style.Position.ABSOLUTE
                this.top = (at * rowHeight - scrollTop - 1.0).px
                this.left = 0.px
            }
            range = IntRange.EMPTY
        }
        dnd.left.listen { removeMarker() }
        dnd.dropped.listen { event ->
            removeMarker()
            val from = (event.data as RowDrag).index
            val at = insertion(event.position.y)
            val to = if (at > from) at - 1 else at
            if (to != from) {
                reordered.trigger(from to to)
                // moving a row can be undone, by moving it back
                editHistory?.record(Edit("Move row", null, { reordered.trigger(to to from) }, { reordered.trigger(from to to) }))
            }
        }
    }

    private fun removeMarker() {
        marker?.let { remove(it); it.close() }
        marker = null
        range = IntRange.EMPTY
    }

    private val top = Div().apply { classes.add(ElementClass("virtual-spacer")) }
    private val bottom = Div().apply { classes.add(ElementClass("virtual-spacer")) }
    private val rows = LinkedHashMap<Int, Div>()

    /** the indices of the rows that exist */
    var range: IntRange = IntRange.EMPTY
        private set

    /** the indices of the rows that are built for the scroll position and the height of the list */
    private fun wanted(): IntRange {
        if (items.isEmpty()) return IntRange.EMPTY
        // before the list is laid out its height is not known, a screenful of rows is built
        val height = layout.screenHeight.takeIf { it > 0.0 } ?: (rowHeight * 20.0)
        val first = (floor(scrollTop / rowHeight).toInt() - overscan).coerceAtLeast(0)
        // the last row that is partly in view
        val last = (ceil((scrollTop + height) / rowHeight).toInt() - 1 + overscan).coerceAtMost(items.size - 1)
        return first..last
    }

    /** builds the rows that come into view and closes those that leave it, before the styles are computed */
    override fun prepare() {
        val wanted = wanted()
        if (wanted == range && children.isNotEmpty()) return
        for (i in rows.keys.filter { it !in wanted }) rows.remove(i)?.close()
        for (i in wanted) {
            if (i !in rows) rows[i] = Div().apply {
                classes.add(ElementClass("virtual-row"))
                style { height = rowHeight.px }
                row(items[i], i)
                if (reorderable) {
                    dnd.data = { RowDrag(this@VirtualList, i) }
                    dnd.label = { rowLabel(items[i]) }
                }
            }
        }
        range = wanted
        top.style { height = (if (wanted.isEmpty()) 0.0 else wanted.first * rowHeight).px }
        bottom.style { height = (if (wanted.isEmpty()) items.size * rowHeight else (items.size - 1 - wanted.last) * rowHeight).px }
        val wantedChildren = listOf(top) + wanted.map { rows[it]!! } + bottom + listOfNotNull(marker)
        if (children != wantedChildren) {
            children.clear()
            for (child in wantedChildren) {
                child.parent = this
                children.add(child)
            }
        }
        // the rows of the items that were in view before the items changed start where
        // they were, once they are in the list
        if (moved.isNotEmpty()) {
            for (i in wanted) {
                val item = items[i]
                val k = moved.indexOfFirst { it.first == item }
                if (k >= 0) rows[i]?.animateMoveFrom(moved.removeAt(k).second)
            }
            moved.clear()
        }
    }

    override fun close() {
        super.close()
        rows.values.forEach { it.close() }
    }
}

/** a [VirtualList] of [items] with rows of [rowHeight] pixels that [row] builds */
fun <T> Element.virtualList(
    items: List<T>,
    rowHeight: Double,
    vararg classes: String,
    row: Div.(item: T, index: Int) -> Unit
): VirtualList<T> = initElement(classes, VirtualList(items, rowHeight, row)) {}
