package org.openrndr.extra.shapes

import org.openrndr.shape.Rectangle

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
): List<List<Rectangle>> {

    val totalWidth = width - marginX * 2.0
    val totalHeight = height - marginY * 2.0

    val totalGutterWidth = gutterX * (columns - 1).coerceAtLeast(0)
    val totalGutterHeight = gutterY * (rows - 1).coerceAtLeast(0)

    val cellWidth = ((totalWidth - totalGutterWidth) / columns).coerceAtLeast(0.0)
    val cellHeight = ((totalHeight - totalGutterHeight) / rows).coerceAtLeast(0.0)

    val cellSpaceX = cellWidth + gutterX
    val cellSpaceY = cellHeight + gutterY

    val x0 = x + marginX
    val y0 = y + marginY

    return (0 until rows).map { row ->
        (0 until columns).map { column ->
            Rectangle(x0 + column * cellSpaceX, y0 + row * cellSpaceY, cellWidth, cellHeight)
        }
    }
}