package org.openrndr.extra.shapes.tunni

import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment
import org.openrndr.shape.intersection

/**
 * Find the Tunni point for the [Segment]
 * @since orx 0.4.5
 */
val Segment.tunniPoint: Vector2
    get() {
        val c = this.cubic
        val ac = LineSegment(c.start, c.control[0])
        val bc = LineSegment(c.end, c.control[1])
        val s = intersection(ac, bc, eps = Double.POSITIVE_INFINITY)
        val t = c.control[0] * 2.0 - start + c.control[1] * 2.0 - end - s
        return t
    }

/**
 * Find the Tunni line for the [Segment]
 * @since orx 0.4.5
 */
val Segment.tunniLine: LineSegment
    get() {
        val c = this.cubic
        return LineSegment(c.control[0], c.control[1])
    }

/**
 * Find a new segment that has [tunniPoint] as its Tunni-point
 * @since orx 0.4.5
 */
fun Segment.withTunniPoint(tunniPoint: Vector2): Segment {
    val ha = (start + tunniPoint) / 2.0
    val hb = (end + tunniPoint) / 2.0
    val hpa = ha + this.cubic.control[1] - end
    val hpb = hb + this.cubic.control[0] - start

    val hahpa = LineSegment(ha, hpa)
    val ac0 = LineSegment(start, this.cubic.control[0])

    val hbhpb = LineSegment(hb, hpb)
    val bc1 = LineSegment(end, this.cubic.control[1])

    val cp0 = intersection(hahpa, ac0, Double.POSITIVE_INFINITY)
    val cp1 = intersection(hbhpb, bc1, Double.POSITIVE_INFINITY)

    return if (cp0 != Vector2.INFINITY && cp1 != Vector2.INFINITY) {
        copy(start = start, control = listOf(cp0, cp1), end = end)
    } else this
}

/**
 * Find a segment for which [pointOnLine] lies on its Tunni-line
 * @since orx 0.4.5
 */
fun Segment.withTunniLine(pointOnLine: Vector2): Segment {
    val ls = LineSegment(pointOnLine, pointOnLine + this.cubic.control[0] - this.cubic.control[1])
    val ac0 = LineSegment(start, this.cubic.control[0])
    val bc1 = LineSegment(end, this.cubic.control[1])

    val cp0 = intersection(ls, ac0, Double.POSITIVE_INFINITY)
    val cp1 = intersection(ls, bc1, Double.POSITIVE_INFINITY)

    return if (cp0 != Vector2.INFINITY && cp1 != Vector2.INFINITY) {
        copy(start, listOf(cp0, cp1), end)
    } else {
        this
    }
}