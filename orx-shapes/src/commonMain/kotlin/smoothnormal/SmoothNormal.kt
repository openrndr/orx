package org.openrndr.extra.shapes.smoothnormal

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Corner neighbourhood of a point on a contour.
 * @property segmentNormal the normal of the segment the point lies on
 * @property leftNormal the end normal of the segment before the corner
 * @property rightNormal the start normal of the segment after the corner
 * @property leftDirection the end direction of the segment before the corner
 * @property w blend position in [0, 1], 0 at the start of the blend region, 0.5 on the corner, 1 at its end
 */
private class CornerBlend(
    val segmentNormal: Vector2,
    val leftNormal: Vector2,
    val rightNormal: Vector2,
    val leftDirection: Vector2,
    val w: Double
)

/**
 * Finds the corner nearest to rectified [t] and calls [blend] when [t] lies within the effective radius of that corner,
 * otherwise returns the normal at [t].
 */
private inline fun RectifiedContour.blendCornerNormal(
    t: Double,
    radius: Double,
    blend: (CornerBlend) -> Vector2
): Vector2 {
    val contour = contour
    if (contour.empty) {
        return Vector2.UNIT_Y
    }
    val segments = contour.segments
    val ownNormal = normal(t)
    if (radius <= 0.0 || segments.size < 2 && !contour.closed) {
        return ownNormal
    }

    val lengths = segments.map { it.length }
    val totalLength = lengths.sum()
    if (totalLength <= 0.0) {
        return ownNormal
    }

    val ts = if (contour.closed) t.mod(1.0) else t.coerceIn(0.0, 1.0)
    val s = ts * totalLength

    // find the segment containing s
    var segmentStart = 0.0
    var index = 0
    while (index < segments.size - 1 && s > segmentStart + lengths[index]) {
        segmentStart += lengths[index]
        index++
    }
    val segmentEnd = segmentStart + lengths[index]

    // pick the nearest corner: the start or end of the segment
    val distanceToStart = s - segmentStart
    val distanceToEnd = segmentEnd - s
    val atStart = distanceToStart <= distanceToEnd

    // indices of the segments before and after the corner
    val (left, right) = if (atStart) Pair(index - 1, index) else Pair(index, index + 1)
    val leftIndex = if (contour.closed) left.mod(segments.size) else left
    val rightIndex = if (contour.closed) right.mod(segments.size) else right
    if (leftIndex !in segments.indices || rightIndex !in segments.indices || leftIndex == rightIndex) {
        return ownNormal
    }

    val effectiveRadius = min(radius, min(lengths[leftIndex], lengths[rightIndex]) * 0.5)
    if (effectiveRadius <= 0.0) {
        return ownNormal
    }

    // signed distance to the corner, negative on the left segment, positive on the right segment
    val d = if (atStart) distanceToStart else -distanceToEnd
    if (abs(d) >= effectiveRadius) {
        return ownNormal
    }

    val leftNormal = segments[leftIndex].normal(1.0, contour.polarity)
    val rightNormal = segments[rightIndex].normal(0.0, contour.polarity)

    // own normal evaluated on the segment found by arc length, the rectified segment lookup may disagree near corners
    val segmentNormal = if (d == 0.0) {
        rightNormal
    } else {
        val (rectifiedSegment, rectifiedOffset) = contour.segment(rectify(ts))
        when {
            rectifiedSegment == index -> segments[index].normal(rectifiedOffset, contour.polarity)
            d < 0.0 -> leftNormal
            else -> rightNormal
        }
    }

    return blend(
        CornerBlend(
            segmentNormal = segmentNormal,
            leftNormal = leftNormal,
            rightNormal = rightNormal,
            leftDirection = segments[leftIndex].direction(1.0),
            w = (d / effectiveRadius + 1.0) * 0.5
        )
    )
}

/**
 * Evaluates a normal at rectified [t] such that it is linearly blended with the normal of the neighbouring segment
 * when [t] lies within [radius] (in arc length) of a corner formed by a segment join.
 *
 * When [t] falls exactly on a corner the returned normal is the average of the normals of the two segments.
 * The effective radius around a corner is limited to half the length of each of the two adjacent segments, such
 * that blend regions of neighbouring corners never overlap.
 *
 * @param t rectified t-value
 * @param radius blend radius in arc length
 * @see smoothNormalAngular
 */
fun RectifiedContour.smoothNormalLinear(t: Double, radius: Double): Vector2 = blendCornerNormal(t, radius) { c ->
    val blended = if (c.w < 0.5) {
        // on the left segment, blend towards the start normal of the right segment
        c.segmentNormal * (1.0 - c.w) + c.rightNormal * c.w
    } else {
        // on the right segment, blend from the end normal of the left segment
        c.leftNormal * (1.0 - c.w) + c.segmentNormal * c.w
    }
    if (blended.squaredLength > 1E-12) blended.normalized else c.segmentNormal
}

/**
 * Evaluates a normal at rectified [t] such that it is rotated towards the normal of the neighbouring segment
 * when [t] lies within [radius] (in arc length) of a corner formed by a segment join.
 *
 * Unlike [smoothNormalLinear] the normal is interpolated by angle, and the blend weight follows a smoothstep curve,
 * such that the normal eases in and out of the blend region. At cusps (corners turning by 180 degrees) the normal
 * rotates around the tip of the cusp.
 *
 * When [t] falls exactly on a corner the returned normal is the angle bisector of the normals of the two segments.
 * The effective radius around a corner is limited to half the length of each of the two adjacent segments, such
 * that blend regions of neighbouring corners never overlap.
 *
 * @param t rectified t-value
 * @param radius blend radius in arc length
 * @see smoothNormalLinear
 */
fun RectifiedContour.smoothNormalAngular(t: Double, radius: Double): Vector2 = blendCornerNormal(t, radius) { c ->
    val w = c.w * c.w * (3.0 - 2.0 * c.w)
    if (c.w < 0.5) {
        // on the left segment, rotate towards the start normal of the right segment
        rotateTowards(c.segmentNormal, c.rightNormal, w, c.leftDirection)
    } else {
        // on the right segment, rotate from the end normal of the left segment
        rotateTowards(c.leftNormal, c.segmentNormal, w, c.leftDirection)
    }
}

/**
 * Rotates [a] towards [b] by fraction [f] of the signed angle between them. When [a] and [b] are opposite the
 * rotation direction is chosen such that it passes through [cuspHint].
 */
private fun rotateTowards(a: Vector2, b: Vector2, f: Double, cuspHint: Vector2): Vector2 {
    val cross = a.x * b.y - a.y * b.x
    val dot = a.dot(b)
    val angle = if (abs(cross) < 1E-9 && dot < 0.0) {
        val side = a.x * cuspHint.y - a.y * cuspHint.x
        if (side < 0.0) -PI else PI
    } else {
        atan2(cross, dot)
    }
    val phi = angle * f
    val c = cos(phi)
    val s = sin(phi)
    return Vector2(a.x * c - a.y * s, a.x * s + a.y * c)
}
