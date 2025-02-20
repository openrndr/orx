package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Rectangle
import kotlin.math.round

/**
 * Splits [Rectangle] into a grid of [Rectangle]s
 * @param columns the number of columns in the resulting grid
 * @param rows the number of rows in the resulting grid
 * @param marginX the unitless margin width
 * @param marginY the unitless margin height
 * @param gutterX the unitless gutter width, the horizontal space between grid cells
 * @param gutterY the unitless gutter height, the vertical space between grid cells
 */
fun Rectangle.grid(
    columns: Int,
    rows: Int,
    marginX: Double = 0.0,
    marginY: Double = 0.0,
    gutterX: Double = 0.0,
    gutterY: Double = 0.0
) = grid(
    (width - marginX * 2 - gutterX * (columns - 1)) / columns,
    (height - marginY * 2 - gutterY * (rows - 1)) / rows,
    marginX, marginY,
    gutterX, gutterY
)

/**
 * Splits [Rectangle] into a grid of [Rectangle]s
 * @param cellWidth the unitless width of a cell
 * @param cellHeight the unitless height of a cell
 * @param minMarginX the unitless minimum margin width (may increase to produce
 * the desired cell aspect ratio)
 * @param minMarginY the unitless minimum margin height (may increase to produce
 * the desired cell aspect ratio)
 * @param gutterX the unitless gutter width, the horizontal space between grid cells
 * @param gutterY the unitless gutter height, the vertical space between grid cells
 */
fun Rectangle.grid(
    cellWidth: Double,
    cellHeight: Double,
    minMarginX: Double = 0.0,
    minMarginY: Double = 0.0,
    gutterX: Double = 0.0,
    gutterY: Double = 0.0
): List<List<Rectangle>> {

    val availableWidth = (width - minMarginX * 2).coerceAtLeast(0.0)
    val availableHeight = (height - minMarginY * 2).coerceAtLeast(0.0)

    val cellSpaceX = cellWidth + gutterX
    val cellSpaceY = cellHeight + gutterY

    val columns = round((availableWidth + gutterX) / cellSpaceX).toInt()
    val rows = round((availableHeight + gutterY) / cellSpaceY).toInt()

    if (columns == 0 || rows == 0) {
        return emptyList()
    }

    val totalGutterWidth = gutterX * (columns - 1).coerceAtLeast(0)
    val totalGutterHeight = gutterY * (rows - 1).coerceAtLeast(0)

    val totalWidth = cellWidth * columns + totalGutterWidth
    val totalHeight = cellHeight * rows + totalGutterHeight

    val x0 = x + (width - totalWidth) / 2
    val y0 = y + (height - totalHeight) / 2

    return (0 until rows).map { row ->
        (0 until columns).map { column ->
            Rectangle(
                x0 + column * cellSpaceX,
                y0 + row * cellSpaceY,
                cellWidth, cellHeight
            )
        }
    }
}

/**
 * Transposes a 2D list of rectangles, switching rows and columns.
 *
 * This method takes a list of lists and rearranges its elements such that
 * the rows become columns and the columns become rows.
 *
 * @return A new 2D list of rectangles where rows and columns are swapped.
 */
fun List<List<Rectangle>>.transpose() : List<List<Rectangle>> {
    val columns = MutableList<MutableList<Rectangle>>(this[0].size) { mutableListOf() }
    for (row in this) {
        for ((index, column) in row.withIndex()) {
            columns[index].add(column)
        }
    }
    return columns
}