package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min

/**
 * Computes the intersection of two rectangles and returns the resulting rectangle.
 * If the rectangles do not intersect, an empty rectangle is returned.
 *
 * @param other The rectangle to intersect with the current rectangle.
 * @return A [Rectangle] representing the overlapping area of the two rectangles,
 * or an empty rectangle if there is no intersection.
 */
fun Rectangle.intersection(other: Rectangle) : Rectangle = if (this.intersects(other)) {
    val tn = this.normalized
    val on = other.normalized

    val left = max(tn.corner.x, on.corner.x)
    val right = min(tn.corner.x + tn.width, on.corner.x + on.width)
    val top = max(tn.corner.y, on.corner.y)
    val bottom = min(tn.corner.y + tn.height, on.corner.y + on.height)

    Rectangle(left, top, right - left, bottom - top)
} else {
    Rectangle.EMPTY
}