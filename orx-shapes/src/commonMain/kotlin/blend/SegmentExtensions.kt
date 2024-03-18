package org.openrndr.extra.shapes.blend

import org.openrndr.math.mix
import org.openrndr.shape.Segment

/**
 * Cubic segment mix
 * @param other the segment to mix with
 * @param f0 the mix factor for the start point
 * @param f1 the mix factor for the first control point
 * @param f2 the mix factor for the second control point
 * @param f3 the mix factor for the end point
 */
fun Segment.mix(other: Segment, f0: Double, f1: Double, f2: Double, f3: Double): Segment {
    val ac = this.cubic
    val bc = other.cubic

    val acc = if (ac.corner) 1.0 else 0.0
    val bcc = if (bc.corner) 1.0 else 0.0

    return Segment(
        ac.start.mix(bc.start, f0),
        ac.control[0].mix(bc.control[0], f1),
        ac.control[1].mix(bc.control[1], f2),
        ac.end.mix(bc.end, f3),
        corner = mix(acc, bcc, f0) >= 0.5
    )
}
