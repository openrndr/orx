package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour

/**
 * A column of a [DataTable]: its [title], its [width] in pixels, the [value] of a row, shown with [format], and the
 * [comparator] that sorts the rows by it, null when the column does not sort
 */
class Column<T>(
    val title: String,
    var width: Double,
    val value: (T) -> Any?,
    val format: (Any?) -> String = { it?.toString() ?: "" },
    val comparator: Comparator<T>? = Comparator { a, b -> compareValues(value(a) as? Comparable<Any>, value(b) as? Comparable<Any>) }
)

/** the direction rows are sorted in */
enum class SortDirection { ASCENDING, DESCENDING }

/**
 * A table of [rows] in [columns], with a header that stays at the top. A click on the title of a column sorts the
 * rows by it, ascending, descending and back to their order; dragging the right edge of a title resizes its column.
 * Only the rows in view are built, see [VirtualList], so long tables are as quick as short ones. A click on a row
 * selects it. The header is a `data-header` of `data-header-cell`s, the rows are `data-row`s, `:selected` when they
 * are selected.
 */
class DataTable<T>(rows: List<T>, val columns: List<Column<T>>, val rowHeight: Double = 24.0) : Div(ElementType("data-table")) {
    class Events<T> : AutoCloseable {
        /** a row is selected */
        val selected = Event<T>("table-selected")

        /** the rows are sorted by a column, null when they are back in their order */
        val sorted = Event<Pair<Column<T>, SortDirection>?>("table-sorted")

        override fun close() {
            selected.close()
            sorted.close()
        }
    }

    val events = Events<T>()

    /** the rows in their order, a change shows them again */
    var rows: List<T> = rows
        set(value) {
            field = value
            refresh()
        }

    /** the column the rows are sorted by and the direction, null when they are in their order */
    var sort: Pair<Column<T>, SortDirection>? = null
        set(value) {
            field = value
            refresh()
            events.sorted.trigger(value)
        }

    /** the row that is selected, null when none is */
    var selection: T? = null
        set(value) {
            if (value != field) {
                field = value
                body.children.flatMap { it.children }.filterIsInstance<DataRow<T>>().forEach { it.updateSelected() }
                value?.let { events.selected.trigger(it) }
            }
        }

    /** the rows as they are shown, sorted */
    var shown: List<T> = rows
        private set

    private val header = Div(ElementType("data-header"))
    private val body: VirtualList<T> = VirtualList(rows, rowHeight) { row, _ -> append(DataRow(this@DataTable, row)) }

    /**
     * True when the rows can be dragged to other places while they are not sorted; the rows are moved and [moved]
     * tells it
     */
    var reorderable: Boolean
        get() = body.reorderable
        set(value) {
            body.reorderable = value
        }

    /** a row was dragged from the first index to the second */
    val moved = Event<Pair<Int, Int>>("table-moved")

    init {
        for (column in columns) header.append(DataHeaderCell(this, column))
        append(header)
        append(body)
        body.rowLabel = { row -> columns.firstOrNull()?.let { it.format(it.value(row)) } ?: row.toString() }
        body.reordered.listen { (from, to) ->
            if (sort != null) return@listen
            this@DataTable.rows = this@DataTable.rows.toMutableList().apply { add(to, removeAt(from)) }
            moved.trigger(from to to)
        }
    }

    private fun refresh() {
        val sorting = sort
        shown = if (sorting == null || sorting.first.comparator == null) rows else {
            val c = sorting.first.comparator!!
            rows.sortedWith(if (sorting.second == SortDirection.ASCENDING) c else c.reversed())
        }
        body.items = shown
        header.children.forEach { it.requestRedraw() }
    }

    /** sorts by [column]: ascending, then descending, then back to the order of the rows */
    fun cycleSort(column: Column<T>) {
        if (column.comparator == null) return
        val current = sort
        sort = when {
            current == null || current.first !== column -> column to SortDirection.ASCENDING
            current.second == SortDirection.ASCENDING -> column to SortDirection.DESCENDING
            else -> null
        }
    }

    /** sets the width of [column], the header and the rows follow */
    fun resize(column: Column<T>, width: Double) {
        column.width = width.coerceAtLeast(24.0)
        header.children.forEach { it.requestLayout(); it.requestRedraw() }
        body.children.flatMap { it.children }.forEach { it.requestRedraw() }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

/** a title of a [DataTable]: it sorts by its column, its right edge resizes it */
class DataHeaderCell<T> internal constructor(private val table: DataTable<T>, val column: Column<T>) : Div(ElementType("data-header-cell")) {
    private var resizing = false
    private var startWidth = 0.0
    private var startX = 0.0

    override val widthHint: Double get() = column.width

    init {
        mouse.pressed.listen {
            val local = it.position.x - layout.screenX
            resizing = local > layout.screenWidth - 6.0
            startWidth = column.width
            startX = it.position.x
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            if (resizing) {
                table.resize(column, startWidth + (it.position.x - startX))
                it.cancelPropagation()
            }
        }
        mouse.clicked.listen {
            if (!resizing) table.cycleSort(column)
            resizing = false
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        super.draw(drawer)
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val h = layout.screenHeight
        val text = panelText ?: return
        val sorting = table.sort?.takeIf { it.first === column }
        text.drawInBox(drawer, this, "title", computedStyle, text.ellipsized(computedStyle, column.title, column.width - 26.0),
            Rectangle(8.0, 0.0, column.width - 26.0, h), 0.0, 0.5, color)
        if (sorting != null) {
            // the direction of the sort
            val c = org.openrndr.math.Vector2(column.width - 12.0, h / 2.0)
            drawer.fill = color
            drawer.stroke = null
            drawer.contour(contour {
                if (sorting.second == SortDirection.ASCENDING) { moveTo(c.x - 4.0, c.y + 2.5); lineTo(c.x + 4.0, c.y + 2.5); lineTo(c.x, c.y - 3.0) }
                else { moveTo(c.x - 4.0, c.y - 2.5); lineTo(c.x + 4.0, c.y - 2.5); lineTo(c.x, c.y + 3.0) }
                close()
            })
        }
        // the edge that resizes
        drawer.fill = color.opacify(0.2)
        drawer.rectangle(column.width - 1.0, 4.0, 1.0, h - 8.0)
    }
}

/** a row of a [DataTable], its cells cut to the widths of their columns */
class DataRow<T> internal constructor(private val table: DataTable<T>, val row: T) : Div(ElementType("data-row")) {
    init {
        updateSelected()
        mouse.clicked.listen {
            table.selection = row
            it.cancelPropagation()
        }
    }

    internal fun updateSelected() {
        if (table.selection == row) pseudoClasses.add(ElementPseudoClass("selected"))
        else pseudoClasses.remove(ElementPseudoClass("selected"))
    }

    override fun draw(drawer: Drawer) {
        super.draw(drawer)
        val text = panelText ?: return
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        var x = 0.0
        for ((i, column) in table.columns.withIndex()) {
            val value = column.format(column.value(row))
            text.drawInBox(drawer, this, "cell $i", computedStyle, text.ellipsized(computedStyle, value, column.width - 16.0),
                Rectangle(x + 8.0, 0.0, column.width - 16.0, layout.screenHeight), 0.0, 0.5, color)
            x += column.width
        }
    }
}

/** a [DataTable] of [rows] in [columns] */
fun <T> Element.dataTable(
    rows: List<T>, columns: List<Column<T>>, vararg classes: String, rowHeight: Double = 24.0,
    init: DataTable<T>.() -> Unit = {}
): DataTable<T> = initElement(classes, DataTable(rows, columns, rowHeight), init)
