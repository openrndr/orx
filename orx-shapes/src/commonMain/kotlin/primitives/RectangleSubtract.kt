package org.openrndr.extra.shapes.primitives

import org.openrndr.math.map
import org.openrndr.shape.Rectangle

/**
 * Splits the current Rectangle into two smaller rectangles at a specified x-coordinate.
 *
 * If the given x-coordinate lies within the range of the current rectangle's width,
 * the method returns two rectangles: one to the left and one to the right of the specified x-coordinate.
 * If the x-coordinate is outside the bounds of the rectangle, the method returns a list
 * containing only the current rectangle.
 *
 * @param x The x-coordinate at which the rectangle is to be split.
 * @return A list of rectangles resulting from the split. The list contains two rectangles if the split
 * occurs within the bounds of the current rectangle, or the original rectangle if the x-coordinate
 * is outside its bounds.
 */
fun Rectangle.splitAtX(x: Double): List<Rectangle> {
    return if (x in xRange) {
        val u = x.map(this.x, this.x + width, 0.0, 1.0)
        listOf(sub(0.0, 0.0, u, 1.0), sub(u, 0.0, 1.0, 1.0))
    } else {
        listOf(this)
    }
}

/**
 * Splits the rectangle horizontally at the specified `y` value if the value lies within the rectangle's vertical range.
 *
 * @param y The vertical value along the y-axis at which to split the rectangle.
 * @return A list of rectangles resulting from the split. If the y value is outside the vertical range,
 * the original rectangle is returned as a single-item list.
 */
fun Rectangle.splitAtY(y: Double): List<Rectangle> {
    return if (y in yRange) {
        val v = y.map(this.y, this.y + height, 0.0, 1.0)
        listOf(sub(0.0, 0.0, 1.0, v), sub(0.0, v, 1.0, 1.0))
    } else {
        listOf(this)
    }
}

/**
 * Subtracts the given rectangle `other` from the current rectangle, splitting and removing
 * overlapping areas and returning the remaining non-overlapping parts as a list of rectangles.
 *
 * @param other The rectangle to subtract from the current rectangle.
 * @return A list of rectangles representing the non-overlapping parts of the current rectangle
 * after the subtraction. If there is no intersection, the list contains only the original rectangle.
 */
fun Rectangle.subtract(other: Rectangle): List<Rectangle> {
    return if (this.intersects(other)) {
        var items = listOf(this)
        items = items.flatMap { it.splitAtX(other.x) }
        items = items.flatMap { it.splitAtY(other.y) }
        items = items.flatMap { it.splitAtX(other.x + other.width) }
        items = items.flatMap { it.splitAtY(other.y + other.height) }
        items = items.filter { it.area > 0 && !it.intersects(other.offsetEdges(-1E-5)) }
        items
    } else {
        listOf(this)
    }
}

/**
 * Subtracts a rectangle from a list of rectangles, removing overlapping areas and returning
 * the non-overlapping parts of the original rectangles.
 *
 * @param other The rectangle to subtract from the list of rectangles.
 * @return A list of rectangles representing the non-overlapping areas from the subtraction
 * of the specified rectangle.
 */
fun List<Rectangle>.subtract(other : Rectangle) : List<Rectangle> {
    return this.flatMap { it.subtract(other) }
}