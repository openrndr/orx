package org.openrndr.extra.shapes.deform

import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment2D

/**
 * Applies a deformation to the line segment by introducing intermediate control points
 * influenced by an attractor vector and a specified strength.
 *
 * @param attractor The vector that acts as the attractor point influencing the deformation.
 * @param strength The strength of the deformation, determining how significantly the line segment bends toward the attractor.
 * @return A new `Segment2D` object with added control points representing the deformed line segment.
 */
fun LineSegment.deform(attractor: Vector2, strength: Double): Segment2D {
    val p1 = start + (end - start) * (1.0 / 3.0) + (attractor - start) * (4 * strength / 3.0)
    val p2 = start + (end - start) * (2.0 / 3.0) + (attractor - end) * (4 * strength / 3.0)
    return Segment2D(start, p1, p2, end)
}

