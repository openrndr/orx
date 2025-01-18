package org.openrndr.extra.shapes.blend

import org.openrndr.math.mix
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D


/**
 * Blends the properties of two `Segment2D` instances based on the provided weights for each control point and the corner property.
 *
 * The resulting `Segment2D` is computed by interpolating between the corresponding properties
 * of `this` segment and the `other` segment, with specific weights for start, control points, end, and corner.
 *
 * @param other the `Segment2D` to blend with the current segment
 * @param f0 the blend factor for the start points
 * @param f1 the blend factor for the first control points
 * @param f2 the blend factor for the second control points
 * @param f3 the blend factor for the end points
 * @return a new `Segment2D` that is the blended result
 */
fun Segment2D.mix(other: Segment2D, f0: Double, f1: Double, f2: Double, f3: Double): Segment2D {
    val ac = this.cubic
    val bc = other.cubic

    val acc = if (ac.corner) 1.0 else 0.0
    val bcc = if (bc.corner) 1.0 else 0.0

    return Segment2D(
        ac.start.mix(bc.start, f0),
        ac.control[0].mix(bc.control[0], f1),
        ac.control[1].mix(bc.control[1], f2),
        ac.end.mix(bc.end, f3),
        corner = mix(acc, bcc, f0) >= 0.5
    )
}

/**
 * Creates a new `Segment3D` by blending the coordinates of two input segments using specified weights.
 *
 * This function performs a weighted blending operation between the start, control, and end points
 * of two cubic `Segment3D` instances, resulting in a new blended segment.
 *
 * @param other the other `Segment3D` to blend with
 * @param f0 the blending weight for the starting point of the segment
 * @param f1 the blending weight for the first control point of the segment
 * @param f2 the blending weight for the second control point of the segment
 * @param f3 the blending weight for the ending point of the segment
 * @return a new `Segment3D` that represents the blended result
 */
fun Segment3D.mix(other: Segment3D, f0: Double, f1: Double, f2: Double, f3: Double): Segment3D {
    val ac = this.cubic
    val bc = other.cubic

    return Segment3D(
        ac.start.mix(bc.start, f0),
        ac.control[0].mix(bc.control[0], f1),
        ac.control[1].mix(bc.control[1], f2),
        ac.end.mix(bc.end, f3),
    )
}
