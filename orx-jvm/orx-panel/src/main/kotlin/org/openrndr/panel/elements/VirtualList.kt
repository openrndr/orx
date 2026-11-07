package org.openrndr.panel.elements

import org.openrndr.panel.style.height
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
            field = value
            rows.values.forEach { it.close() }
            rows.clear()
            range = IntRange.EMPTY
            requestLayout()
        }

    /** the rows that are built around the rows in view, above and below */
    var overscan = 4

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
            }
        }
        range = wanted
        top.style { height = (if (wanted.isEmpty()) 0.0 else wanted.first * rowHeight).px }
        bottom.style { height = (if (wanted.isEmpty()) items.size * rowHeight else (items.size - 1 - wanted.last) * rowHeight).px }
        val wantedChildren = listOf(top) + wanted.map { rows[it]!! } + bottom
        if (children != wantedChildren) {
            children.clear()
            for (child in wantedChildren) {
                child.parent = this
                children.add(child)
            }
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
