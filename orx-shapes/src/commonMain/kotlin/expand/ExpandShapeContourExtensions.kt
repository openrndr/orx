package org.openrndr.extra.shapes.expand

import offset.arcSegments
import offset.offset
import offset.offsetCurve
import offset.withoutTinySegments
import org.openrndr.draw.LineCap
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.difference
import org.openrndr.shape.removeSelfIntersections

/**
 * Expands this contour into the filled outline of a stroke of width [weight].
 *
 * An open contour is offset by half the [weight] to both sides, the two offsets are connected at the ends with caps of
 * type [cap] and the resulting closed outline is resolved with [ShapeContour.removeSelfIntersections], such that
 * stretches where the stroke overlaps itself are merged. A closed contour expands to the ring between its outward and
 * inward offsets, [cap] is not used then.
 *
 * @param weight the width of the stroke
 * @param join how the offsets are joined at sharp corners of the contour
 * @param cap how the stroke is closed at the ends of an open contour
 * @param errorTolerance maximum fit error of the offset curves
 * @param sampleDistance distance along the contour between samples used for fitting the offset curves
 */
fun ShapeContour.expand(
    weight: Double,
    join: SegmentJoin = SegmentJoin.MITER,
    cap: LineCap = LineCap.SQUARE,
    errorTolerance: Double = 0.05,
    sampleDistance: Double = 1.0
): Shape {
    if (empty || weight <= 0.0) {
        return Shape(emptyList())
    }
    val halfWeight = weight / 2.0

    if (closed) {
        val outer = offset(halfWeight, join, errorTolerance, sampleDistance)
        val inner = offset(-halfWeight, join, errorTolerance, sampleDistance)
        return if (inner.empty) outer else outer.difference(inner)
    }

    val left = offsetCurve(halfWeight, join, errorTolerance, sampleDistance)
    val right = offsetCurve(-halfWeight, join, errorTolerance, sampleDistance).reversed
    if (left.empty || right.empty) {
        return Shape(emptyList())
    }

    val segments = mutableListOf<Segment2D>()
    fun append(segment: Segment2D) {
        if (segment.length < 1E-9) {
            return
        }
        val last = segments.lastOrNull()
        if (last != null && last.end != segment.start) {
            if (last.end.distanceTo(segment.start) < 1E-6) {
                segments.add(segment.copy(start = last.end))
                return
            }
            segments.add(Segment2D(last.end, segment.start))
        }
        segments.add(segment)
    }

    val start = this.segments.first()
    val end = this.segments.last()

    left.segments.forEach { append(it) }
    capSegments(left.segments.last().end, right.segments.first().start, end.end, end.direction(1.0), halfWeight, cap)
        .forEach { append(it) }
    right.segments.forEach { append(it) }
    capSegments(right.segments.last().end, left.segments.first().start, start.start, start.direction(0.0) * -1.0, halfWeight, cap)
        .forEach { append(it) }

    // close exactly onto the first segment
    val first = segments.first()
    val last = segments.last()
    if (last.end != first.start) {
        if (last.end.distanceTo(first.start) < 1E-6) {
            segments[segments.size - 1] = last.copy(end = first.start)
        } else {
            segments.add(Segment2D(last.end, first.start))
        }
    }

    val outline = ShapeContour(segments, true, polarity).withoutTinySegments()
    return if (outline.empty) Shape(emptyList()) else outline.removeSelfIntersections()
}

/**
 * The segments of a cap from [from] to [to] around the contour end point [center], bulging out in direction
 * [outward].
 */
private fun capSegments(
    from: Vector2,
    to: Vector2,
    center: Vector2,
    outward: Vector2,
    halfWeight: Double,
    cap: LineCap
): List<Segment2D> {
    return when (cap) {
        LineCap.BUTT -> listOf(Segment2D(from, to))
        LineCap.SQUARE -> {
            val extension = outward.normalized * halfWeight
            listOf(
                Segment2D(from, from + extension),
                Segment2D(from + extension, to + extension),
                Segment2D(to + extension, to)
            )
        }
        LineCap.ROUND -> {
            // two quarter arcs through the apex, a half circle has no unique short way round
            val apex = center + outward.normalized * halfWeight
            arcSegments(center, from, apex, halfWeight) + arcSegments(center, apex, to, halfWeight)
        }
    }
}
