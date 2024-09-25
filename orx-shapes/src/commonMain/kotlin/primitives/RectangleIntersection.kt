package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min

/**
 * Find intersection of [this] and [other]
 * @return a rectangle shaped intersection or [Rectangle.EMPTY] when the intersection is empty.
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