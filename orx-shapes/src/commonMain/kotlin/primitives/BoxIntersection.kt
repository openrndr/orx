package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Box
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min

/**
 * Computes the intersection of the current box with another box.
 * If the two boxes intersect, the resulting box represents the overlapping region.
 * If the two boxes do not intersect, an empty box is returned.
 *
 * @param other The box to intersect with the current box.
 * @return A new box representing the overlapping region between the current box and the specified box,
 *         or an empty box if there is no intersection.
 */
fun Box.intersection(other: Box) : Box = if (this.intersects(other)) {
    val tn = this.normalized
    val on = other.normalized

    val left = max(tn.corner.x, on.corner.x)
    val right = min(tn.corner.x + tn.width, on.corner.x + on.width)
    val top = max(tn.corner.y, on.corner.y)
    val bottom = min(tn.corner.y + tn.height, on.corner.y + on.height)
    val near = max(tn.corner.z, on.corner.z)
    val far = min(tn.corner.z + tn.depth, on.corner.z + on.depth)

    Box(left, top, near, right - left, bottom - top, far - near)
} else {
    Box.EMPTY
}