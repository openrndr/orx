package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.jvm.JvmName
import kotlin.math.round
import kotlin.random.Random

/**
 * Divides a rectangle into a grid of sub-rectangles with irregular spacing,
 * based on the specified column and row weights. Optionally, margins can be
 * applied on both the horizontal and vertical directions.
 *
 * @param columnWeights A list of relative weights for the columns. The size
 * of this list determines the number of columns, and each weight defines
 * the proportional width of the respective column.
 * @param rowWeights A list of relative weights for the rows. The size of
 * this list determines the number of rows, and each weight defines the
 * proportional height of the respective row.
 * @param marginX The horizontal margin between the edges of the main rectangle
 * and the grid. Defaults to 0.0.
 * @param marginY The vertical margin between the edges of the main rectangle
 * and the grid. Defaults to 0.0.
 * @return A list of lists, where each sublist represents a row of the grid,
 * and each element within the row is a sub-rectangle corresponding to a cell
 * in the grid.
 */
fun Rectangle.irregularGrid(
    columnWeights: List<Double>,
    rowWeights: List<Double>,
    marginX: Double = 0.0,
    marginY: Double = 0.0,
): List<List<Rectangle>> {

    val columnWeight = columnWeights.sum()
    val rowWeight = rowWeights.sum()

    val columnRatios = columnWeights.map { it / columnWeight }
    val rowRatios = rowWeights.map { it / rowWeight }

    val us = columnRatios.scan(0.0) { acc, d -> acc + d }
    val vs = rowRatios.scan(0.0) { acc, d -> acc + d }

    val result = mutableListOf<MutableList<Rectangle>>()

    val withMargins = this.offsetEdges(-marginX, -marginY)

    for (j in 0 until vs.size - 1) {
        val v0 = vs[j]
        val v1 = vs[j + 1]
        val row = mutableListOf<Rectangle>()
        for (i in 0 until us.size - 1) {
            val u0 = us[i]
            val u1 = us[i + 1]
            row.add(withMargins.sub(u0, v0, u1, v1))
        }
        result.add(row)
    }
    return result
}


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
fun List<List<Rectangle>>.transpose(): List<List<Rectangle>> {
    val columns = MutableList<MutableList<Rectangle>>(this[0].size) { mutableListOf() }
    for (row in this) {
        for ((index, column) in row.withIndex()) {
            columns[index].add(column)
        }
    }
    return columns
}

/**
 * Retrieves a [Rectangle] from a two-dimensional list of [Rectangle]s based on
 * the specified x and y indices.
 *
 * @param x The column index in the two-dimensional list.
 * @param y The row index in the two-dimensional list.
 * @return The [Rectangle] at the specified indices (x, y).
 */
operator fun List<List<Rectangle>>.get(x: Int, y: Int): Rectangle = this[y][x]


/**
 * Retrieves a sublist of [Rectangle] objects from a two-dimensional [List] given a range of indices for rows and a specific column index.
 *
 * @param xRange The range of indices specifying the columns to be sliced.
 * @param y The index of the row from which the sublist is retrieved.
 * @return A sublist of [Rectangle] objects within the specified range of columns from the specified row.
 */
operator fun List<List<Rectangle>>.get(xRange: IntRange, y: Int): List<Rectangle> = this[y].slice(xRange)

/**
 * Retrieves a list of rectangles at a specific x-coordinate for a range of y-coordinates
 * from a 2D list of rectangles.
 *
 * @param x The x-coordinate to access within each inner list.
 * @param yRange The range of y-coordinates (indices of the outer list) to retrieve rectangles from.
 * @return A list of rectangles corresponding to the specified x-coordinate and y-coordinate range.
 */
operator fun List<List<Rectangle>>.get(x: Int, yRange: IntRange): List<Rectangle> = yRange.map { y -> this[y][x] }

/**
 * Retrieves a subgrid from a 2D list of [Rectangle]s based on the specified ranges.
 *
 * @param xRange The range of x indices to include in the subgrid.
 * @param yRange The range of y indices to include in the subgrid.
 * @return A 2D list containing the elements of the subgrid specified by the ranges.
 */
operator fun List<List<Rectangle>>.get(xRange: IntRange, yRange: IntRange): List<List<Rectangle>> =
    yRange.map { y -> xRange.map { x -> this[y][x] } }


/**
 * Computes the bounding rectangle that encompasses all the rectangles contained in the lists.
 *
 * This property traverses the two-dimensional list structure to compute the bounds of each
 * individual rectangle, ultimately returning a single [Rectangle] that encompasses
 * all the rectangles in all nested lists.
 *
 * If the list is empty or contains no rectangles, the resulting bounds might be undefined,
 * depending on the behavior of the nested bounds calculations.
 */
val List<List<Rectangle>>.bounds: Rectangle
    @JvmName("getRectangleListBounds") get() {
        val bounds = map { it.bounds }
        return bounds.bounds
    }

/**
 * Selects a random [Rectangle] from a nested list of rectangles using the provided random generator.
 *
 * @param random An instance of [Random] used to select rectangles in a random manner.
 * @return A randomly selected [Rectangle] from the nested list.
 */
fun List<List<Rectangle>>.uniform(random: Random): Rectangle {
    return this.random(random).random(random)
}

/**
 * Retrieves a column of rectangles from a 2D list of rectangles.
 *
 * @param index The index of the column to retrieve.
 * @return A list of [Rectangle] objects representing the specified column.
 */
fun List<List<Rectangle>>.column(index: Int): List<Rectangle> = this.map { it[index] }

/**
 * Retrieves the row from a 2D list of `Rectangle` objects at the specified index.
 *
 * @receiver The 2D list of `Rectangle` objects.
 * @param index The index of the row to retrieve. Must be in the valid range of indices for the list.
 * @return A list of `Rectangle` objects representing the row at the given index.
 */
fun List<List<Rectangle>>.row(index: Int): List<Rectangle> = this[index]