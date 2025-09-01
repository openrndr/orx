package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Box
import kotlin.math.round

/**
 * Split [Box] into a grid of [Box]es
 * @param columns the number of columns in the resulting grid
 * @param rows the number of rows in the resulting grid
 * @param marginX the unitless margin width
 * @param marginY the unitless margin height
 * @param marginZ the untless margin depth
 * @param gutterX the unitless gutter width, the horizontal space between grid cells
 * @param gutterY the unitless gutter height, the vertical space between grid cells
 * @param gutterZ the unitless gutter depth
 */
fun Box.grid(
    columns: Int,
    rows: Int,
    slices: Int,
    marginX: Double = 0.0,
    marginY: Double = 0.0,
    marginZ: Double = 0.0,
    gutterX: Double = 0.0,
    gutterY: Double = 0.0,
    gutterZ: Double = 0.0
) = grid(
    (width - marginX * 2 - gutterX * (columns - 1)) / columns,
    (height - marginY * 2 - gutterY * (rows - 1)) / rows,
    (depth - marginZ * 2 - gutterZ * (slices - 1)) / slices,
    marginX, marginY, marginZ,
    gutterX, gutterY, gutterZ
)

/**
 * Split [Box] into a grid of [Box]es
 * @param cellWidth the unitless width of a cell
 * @param cellHeight the unitless height of a cell
 * @param cellDepth the unitless depth of a cell
 * @param minMarginX the unitless minimum margin width (may increase to produce
 * the desired cell aspect ratio)
 * @param minMarginY the unitless minimum margin height (may increase to produce
 * the desired cell aspect ratio)
 * @param minMarginZ the unitless minimum margin depth (may increase to produce
 * @param gutterX the unitless gutter width, the horizontal space between grid cells
 * @param gutterY the unitless gutter height, the vertical space between grid cells
 * @param gutterZ the unitless gutter depth
 */
fun Box.grid(
    cellWidth: Double,
    cellHeight: Double,
    cellDepth: Double,
    minMarginX: Double = 0.0,
    minMarginY: Double = 0.0,
    minMarginZ: Double = 0.0,
    gutterX: Double = 0.0,
    gutterY: Double = 0.0,
    gutterZ: Double = 0.0
): List<List<List<Box>>> {

    val availableWidth = (width - minMarginX * 2).coerceAtLeast(0.0)
    val availableHeight = (height - minMarginY * 2).coerceAtLeast(0.0)
    val availableDepth = (depth - minMarginZ * 2).coerceAtLeast(0.0)

    val cellSpaceX = cellWidth + gutterX
    val cellSpaceY = cellHeight + gutterY
    val cellSpaceZ = cellDepth + gutterZ

    val columns = round((availableWidth + gutterX) / cellSpaceX).toInt()
    val rows = round((availableHeight + gutterY) / cellSpaceY).toInt()
    val slices = round((availableDepth + gutterZ) / cellSpaceZ).toInt()

    if (columns == 0 || rows == 0 || slices == 0) {
        return emptyList()
    }

    val totalGutterWidth = gutterX * (columns - 1).coerceAtLeast(0)
    val totalGutterHeight = gutterY * (rows - 1).coerceAtLeast(0)
    val totalGutterDepth = gutterZ * (slices - 1).coerceAtLeast(0)

    val totalWidth = cellWidth * columns + totalGutterWidth
    val totalHeight = cellHeight * rows + totalGutterHeight
    val totalDepth = cellDepth * slices + totalGutterDepth

    val x0 = corner.x + (width - totalWidth) / 2
    val y0 = corner.y + (height - totalHeight) / 2
    val z0 = corner.z + (depth - totalDepth) / 2

    return (0 until slices).map { slice ->
        (0 until rows).map { row ->
            (0 until columns).map { column ->
                Box(
                    x0 + column * cellSpaceX,
                    y0 + row * cellSpaceY,
                    z0 + slice * cellSpaceZ,
                    cellWidth, cellHeight, cellDepth
                )
            }
        }
    }
}
