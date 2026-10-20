package org.openrndr.extra.shapes.splines

import org.openrndr.math.LinearType
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.YPolarity
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import org.openrndr.shape.ShapeContour

/**
 * The tension, continuity and bias of a [kochanekBartels] spline at a point.
 *
 * @param tension how tightly the curve bends at the point: 1 gives zero tangents, so the curve runs straight into and
 * out of the point; negative values give rounder curves
 * @param continuity how sharply the direction changes at the point: -1 gives a corner along the incoming and outgoing
 * edges, 1 an inverted corner, 0 a smooth curve
 * @param bias the direction of the tangent at the point: 1 along the incoming edge, so that the curve overshoots after
 * the point, -1 along the outgoing edge, so that it overshoots before it, 0 their average
 */
data class TCB(val tension: Double = 0.0, val continuity: Double = 0.0, val bias: Double = 0.0)

/**
 * Converts this polyline into a Kochanek-Bartels spline, after Kochanek and Bartels, "Interpolating splines with local
 * tension, continuity, and bias control" (SIGGRAPH 1984): a cubic Hermite spline through all points whose tangents
 * are shaped by a tension, continuity and bias, see [TCB]. With all three 0 it is the uniform Catmull-Rom spline.
 *
 * At point pᵢ, with Δ₋ = pᵢ - pᵢ₋₁ and Δ₊ = pᵢ₊₁ - pᵢ, the curve arrives with tangent
 * (1-t)(1-c)(1+b)/2·Δ₋ + (1-t)(1+c)(1-b)/2·Δ₊ and leaves with tangent (1-t)(1+c)(1+b)/2·Δ₋ + (1-t)(1-c)(1-b)/2·Δ₊.
 * The two are equal, so the curve is C1, only for continuity 0. Every edge is parametrized over [0, 1], as in the
 * paper without its adjustment for uneven keyframe spacing. The ends of an open polyline take the missing edge as the
 * mirror image of the edge next to it, so with all parameters 0 the curve leaves the first point along the first edge.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param tension the tension at every point
 * @param continuity the continuity at every point
 * @param bias the bias at every point
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.kochanekBartels(
    closed: Boolean,
    tension: Double = 0.0,
    continuity: Double = 0.0,
    bias: Double = 0.0,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour = kochanekBartels(closed, List(size) { TCB(tension, continuity, bias) }, polarity)

/**
 * Converts this polyline into a Kochanek-Bartels spline with a tension, continuity and bias per point, see
 * [List.kochanekBartels].
 *
 * @param parameters the tension, continuity and bias at every point
 */
fun List<Vector2>.kochanekBartels(
    closed: Boolean,
    parameters: List<TCB>,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    if (size < 2) return ShapeContour.fromPoints(this, closed, polarity)
    val segments = hermiteControlPoints(this, closed, parameters).map { (p0, c0, c1, p1) -> Segment2D(p0, c0, c1, p1) }
    return ShapeContour.fromSegments(segments, closed, polarity)
}

/**
 * Converts this 3D polyline into a Kochanek-Bartels spline, for instance for a camera path, see [List.kochanekBartels].
 */
fun List<Vector3>.kochanekBartels(
    closed: Boolean,
    tension: Double = 0.0,
    continuity: Double = 0.0,
    bias: Double = 0.0
): Path3D = kochanekBartels(closed, List(size) { TCB(tension, continuity, bias) })

/** Converts this 3D polyline into a Kochanek-Bartels spline with a tension, continuity and bias per point. */
fun List<Vector3>.kochanekBartels(closed: Boolean, parameters: List<TCB>): Path3D {
    if (size < 2) return Path3D.fromPoints(this, closed)
    val segments = hermiteControlPoints(this, closed, parameters).map { (p0, c0, c1, p1) ->
        Segment3D(p0, listOf(c0, c1), p1)
    }
    return Path3D.fromSegments(segments, closed)
}

/** The Bézier control points of every edge of the Kochanek-Bartels spline through [points]. */
internal fun <T : LinearType<T>> hermiteControlPoints(
    points: List<T>,
    closed: Boolean,
    parameters: List<TCB>
): List<List<T>> {
    require(parameters.size == points.size) { "expected ${points.size} parameters, got ${parameters.size}" }
    val n = points.size
    val incoming = ArrayList<T>(n)
    val outgoing = ArrayList<T>(n)
    for (i in 0 until n) {
        val (t, c, b) = parameters[i]
        val after = when {
            i < n - 1 -> points[i + 1] - points[i]
            closed -> points[0] - points[i]
            else -> points[i] - points[i - 1]
        }
        val before = when {
            i > 0 -> points[i] - points[i - 1]
            closed -> points[i] - points[n - 1]
            else -> after
        }
        incoming.add(before * ((1 - t) * (1 - c) * (1 + b) / 2) + after * ((1 - t) * (1 + c) * (1 - b) / 2))
        outgoing.add(before * ((1 - t) * (1 + c) * (1 + b) / 2) + after * ((1 - t) * (1 - c) * (1 - b) / 2))
    }
    val edges = if (closed) n else n - 1
    return List(edges) { i ->
        val j = (i + 1) % n
        listOf(points[i], points[i] + outgoing[i] / 3.0, points[j] - incoming[j] / 3.0, points[j])
    }
}
