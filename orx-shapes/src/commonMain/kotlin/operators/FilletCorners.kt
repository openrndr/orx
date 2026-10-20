package org.openrndr.extra.shapes.operators

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Converts this polyline into a contour in which the corners are replaced by circular arcs of [radius] that keep the
 * tangent direction continuous along the whole contour.
 *
 * Every corner gets an arc that is tangent to both adjacent edges (a fillet). Where two consecutive corners turn in
 * opposite directions and their fillets don't fit on the edge between them, and [reverseCurves] is true, the pair is
 * replaced by a reverse curve (S-bend): two arcs of [radius] that turn in opposite directions and meet tangentially at
 * an inflection point on the middle edge. The incoming and outgoing edges are kept, the middle edge is replaced. At the
 * point where the fillets just fit, the S-bend coincides with them.
 *
 * Arcs that still don't fit next to their neighbours are shrunk: the length of every edge is shared between the arcs at
 * its two ends in proportion to what they need. Fillets of same-direction corners on short edges, and all fillets when
 * [reverseCurves] is false, therefore get a smaller radius. For open polylines the first and last edge can be consumed
 * entirely by their single arc. Collinear corners are left as they are and corners that reverse direction (a 180 degree
 * turn) can't be filleted and stay sharp.
 *
 * Arcs are approximated with one cubic Bézier per 90 degrees of turn, the radial error is below 0.03% of the radius.
 *
 * @param radius the arc radius
 * @param closed whether the polyline is closed, the last vertex should then not repeat the first
 * @param reverseCurves whether pairs of opposite corners on a short edge are joined by an S-bend of two opposite arcs
 * instead of two shrunk fillets
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.filletCorners(
    radius: Double,
    closed: Boolean,
    reverseCurves: Boolean = true,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    val points = filterIndexed { i, p ->
        val next = if (i + 1 < size) this[i + 1] else if (closed) this[0] else null
        next == null || p.squaredDistanceTo(next) > 1E-18
    }
    if (points.size < 2 || (closed && points.size < 3) || radius <= 0.0) {
        return ShapeContour.fromPoints(points, closed, polarity)
    }
    return Filleter(points, closed, radius, reverseCurves).contour(polarity)
}

private fun perpendicular(v: Vector2) = Vector2(-v.y, v.x)

private fun rotate(v: Vector2, a: Double) = Vector2(v.x * cos(a) - v.y * sin(a), v.x * sin(a) + v.y * cos(a))

/** The angle by which direction [a] has to turn to reach direction [b] on [side] (1: counterclockwise), in [0, 2π). */
private fun sweep(a: Vector2, b: Vector2, side: Double): Double {
    val angle = side * atan2(a.x * b.y - a.y * b.x, a.dot(b))
    return if (angle < 0.0) angle + 2 * PI else angle
}

/**
 * Cubic Bézier approximation of a circular arc around [center] from [start] with direction [startDirection] to [end]
 * with direction [endDirection], turning by [angle] radians on [side].
 */
private fun arcSegments(
    start: Vector2, startDirection: Vector2,
    end: Vector2, endDirection: Vector2,
    center: Vector2, side: Double, angle: Double, radius: Double
): List<Segment2D> {
    val pieces = ceil(angle / (PI / 2.0) - 1E-9).toInt().coerceAtLeast(1)
    val step = angle / pieces
    val handle = 4.0 / 3.0 * tan(step / 4.0) * radius
    val result = mutableListOf<Segment2D>()
    var p0 = start
    var d0 = startDirection
    for (j in 1..pieces) {
        val a = side * step * j
        val p1 = if (j == pieces) end else center + rotate(start - center, a)
        val d1 = if (j == pieces) endDirection else rotate(startDirection, a)
        result.add(Segment2D(p0, p0 + d0 * handle, p1 - d1 * handle, p1))
        p0 = p1
        d0 = d1
    }
    return result
}

/**
 * A rounded replacement of one corner, or of two consecutive corners for an S-bend, from [entry] on the incoming edge
 * to [exit] on the outgoing edge.
 */
private class Feature(val entry: Vector2, val exit: Vector2, val segments: List<Segment2D>)

/**
 * An S-bend between the line through [p] with direction [u] and the line through [q] with direction [w], with its
 * inflection point on the line through [p] and [q]. [a] is the position of the entry along the incoming line relative
 * to [p] (negative before it), [b] the position of the exit along the outgoing line relative to [q].
 */
private class ReverseCurve(
    val a: Double, val b: Double,
    val c1: Vector2, val c2: Vector2,
    val inflection: Vector2, val direction: Vector2,
    val sweep1: Double, val sweep2: Double
)

private class Filleter(val points: List<Vector2>, val closed: Boolean, val radius: Double, val reverseCurves: Boolean) {
    val n = points.size
    val edgeCount = if (closed) n else n - 1

    // edge e runs from points[e] to points[(e + 1) % n]
    val length = DoubleArray(edgeCount) { points[it].distanceTo(points[(it + 1) % n]) }
    val direction = List(edgeCount) { (points[(it + 1) % n] - points[it]) / length[it] }

    // corner i sits at points[i], between edge i - 1 and edge i; open polylines have no corner at their ends
    val corners = if (closed) 0 until n else 1 until n - 1
    val turn = DoubleArray(n)
    val side = DoubleArray(n)
    val tangent = DoubleArray(n)

    fun incoming(i: Int) = (i - 1 + edgeCount) % edgeCount
    fun outgoing(i: Int) = i % edgeCount
    fun next(i: Int) = (i + 1) % n

    init {
        for (i in corners) {
            val u = direction[incoming(i)]
            val v = direction[outgoing(i)]
            val angle = acos(u.dot(v).coerceIn(-1.0, 1.0))
            if (angle < 1E-9 || angle > PI - 1E-9) continue
            turn[i] = angle
            side[i] = if (u.x * v.y - u.y * v.x > 0.0) 1.0 else -1.0
            tangent[i] = radius * tan(angle / 2.0)
        }
    }

    fun isCorner(i: Int) = turn[i] > 0.0

    /**
     * Solves the S-bend of radius [r] for the opposite corners [i] and [i] + 1. The arc centers lie at [r] from the
     * incoming and the outgoing line, [r] from the inflection point, which must lie on the middle edge line. Of the (up
     * to two) solutions the one with valid arcs whose inflection point is closest to where the fillets would meet is
     * returned.
     */
    fun reverseCurve(i: Int, r: Double): ReverseCurve? {
        val j = next(i)
        val p = points[i]
        val q = points[j]
        val u = direction[incoming(i)]
        val d = direction[outgoing(i)]
        val w = direction[outgoing(j)]
        val s1 = side[i]
        val s2 = side[j]

        // c1 = k + u * a, c2 = l + w * b
        val k = p + perpendicular(u) * (s1 * r)
        val l = q + perpendicular(w) * (s2 * r)
        val nd = perpendicular(d)
        val ndw = nd.dot(w)
        if (abs(ndw) < 1E-12) return null
        // inflection (c1 + c2) / 2 on the middle line: b = b0 + b1 * a
        val b0 = -nd.dot(k + l - p * 2.0) / ndw
        val b1 = -nd.dot(u) / ndw
        // |c2 - c1| = 2r with c2 - c1 = g + h * a
        val g = l - k + w * b0
        val h = w * b1 - u
        val qa = h.dot(h)
        val qb = 2.0 * g.dot(h)
        val qc = g.dot(g) - 4.0 * r * r
        val roots = if (qa < 1E-12) {
            if (abs(qb) < 1E-12) emptyList() else listOf(-qc / qb)
        } else {
            val discriminant = qb * qb - 4.0 * qa * qc
            if (discriminant < 0.0) emptyList() else {
                val root = sqrt(discriminant)
                listOf((-qb - root) / (2.0 * qa), (-qb + root) / (2.0 * qa))
            }
        }

        val expected = p + d * (length[outgoing(i)] * tangent[i] / (tangent[i] + tangent[j]))
        var best: ReverseCurve? = null
        var bestDistance = Double.POSITIVE_INFINITY
        for (a in roots) {
            val b = b0 + b1 * a
            if (-a < -1E-9 || b < -1E-9) continue
            val c1 = k + u * a
            val c2 = l + w * b
            val inflection = (c1 + c2) * 0.5
            val e = (perpendicular(inflection - c1) * s1).normalized
            val sweep1 = sweep(u, e, s1)
            val sweep2 = sweep(e, w, s2)
            if (sweep1 < 1E-9 || sweep1 > PI - 1E-9 || sweep2 < 1E-9 || sweep2 > PI - 1E-9) continue
            val distance = inflection.squaredDistanceTo(expected)
            if (distance < bestDistance) {
                bestDistance = distance
                best = ReverseCurve(a, b, c1, c2, inflection, e, sweep1, sweep2)
            }
        }
        return best
    }

    fun fillet(i: Int, scale: Double): Feature {
        val t = tangent[i] * scale
        if (t < 1E-12) return Feature(points[i], points[i], emptyList())
        val u = direction[incoming(i)]
        val v = direction[outgoing(i)]
        val r = t / tan(turn[i] / 2.0)
        val start = points[i] - u * t
        val end = points[i] + v * t
        val center = start + perpendicular(u) * (side[i] * r)
        return Feature(start, end, arcSegments(start, u, end, v, center, side[i], turn[i], r))
    }

    fun sBend(i: Int, curve: ReverseCurve, r: Double): Feature {
        val j = next(i)
        val u = direction[incoming(i)]
        val w = direction[outgoing(j)]
        val start = points[i] + u * curve.a
        val end = points[j] + w * curve.b
        val segments = arcSegments(start, u, curve.inflection, curve.direction, curve.c1, side[i], curve.sweep1, r) +
                arcSegments(curve.inflection, curve.direction, end, w, curve.c2, side[j], curve.sweep2, r)
        return Feature(start, end, segments)
    }

    fun features(): List<Feature> {
        // pair opposite corners whose fillets don't fit on their shared edge, most overfull edges first
        val pairedWith = IntArray(n) { -1 }
        if (reverseCurves) {
            val candidates = (0 until edgeCount).filter { i ->
                val j = next(i)
                isCorner(i) && isCorner(j) && side[i] != side[j] &&
                        tangent[i] + tangent[j] > length[i]
            }.sortedByDescending { (tangent[it] + tangent[next(it)]) / length[it] }
            for (i in candidates) {
                val j = next(i)
                if (pairedWith[i] != -1 || pairedWith[j] != -1) continue
                if (reverseCurve(i, radius) == null) continue
                pairedWith[i] = j
                pairedWith[j] = i
            }
        }
        val isSBend = BooleanArray(n) { pairedWith[it] == next(it) }

        // the lengths the features at both ends of every edge claim, at full radius
        val claimStart = DoubleArray(edgeCount)
        val claimEnd = DoubleArray(edgeCount)
        val fullCurves = arrayOfNulls<ReverseCurve>(n)
        for (i in corners) {
            if (pairedWith[i] == -1) {
                claimStart[outgoing(i)] = tangent[i]
                claimEnd[incoming(i)] = tangent[i]
            } else if (isSBend[i]) {
                val curve = reverseCurve(i, radius)!!
                fullCurves[i] = curve
                claimEnd[incoming(i)] = -curve.a
                claimStart[outgoing(next(i))] = curve.b
            }
        }
        // shared edges are divided in proportion to the claims
        fun allowance(claim: Double, other: Double, e: Int) =
            if (claim + other > length[e]) length[e] * claim / (claim + other) else Double.POSITIVE_INFINITY

        val allowStart = DoubleArray(edgeCount) { allowance(claimStart[it], claimEnd[it], it) }
        val allowEnd = DoubleArray(edgeCount) { allowance(claimEnd[it], claimStart[it], it) }

        fun filletScale(i: Int) = if (tangent[i] == 0.0) 1.0 else
            min(1.0, min(allowStart[outgoing(i)], allowEnd[incoming(i)]) / tangent[i])

        val result = mutableListOf<Feature>()
        for (i in corners) {
            when {
                isSBend[i] -> {
                    val j = next(i)
                    fun fits(curve: ReverseCurve?) =
                        curve != null && -curve.a <= allowEnd[incoming(i)] + 1E-9 && curve.b <= allowStart[outgoing(j)] + 1E-9

                    val full = fullCurves[i]!!
                    if (fits(full)) {
                        result.add(sBend(i, full, radius))
                        continue
                    }
                    // shrink the S-bend, it turns into two fillets at the scale where those just fit
                    val minimum = length[outgoing(i)] / (tangent[i] + tangent[j])
                    var lower = minimum
                    var upper = 1.0
                    var best = -1.0
                    repeat(40) {
                        val scale = (lower + upper) / 2.0
                        if (fits(reverseCurve(i, radius * scale))) {
                            best = scale
                            lower = scale
                        } else {
                            upper = scale
                        }
                    }
                    if (best > 0.0) {
                        result.add(sBend(i, reverseCurve(i, radius * best)!!, radius * best))
                    } else {
                        result.add(fillet(i, min(minimum, filletScale(i))))
                        result.add(fillet(j, min(minimum, filletScale(j))))
                    }
                }

                pairedWith[i] != -1 -> Unit // the second corner of an S-bend

                isCorner(i) -> result.add(fillet(i, filletScale(i)))

                else -> result.add(Feature(points[i], points[i], emptyList()))
            }
        }
        // an S-bend over the closing corner of a closed polyline starts at the last corner, so it is already last
        return result
    }

    fun contour(polarity: YPolarity): ShapeContour {
        val features = features()
        // gaps this small are numerical (e.g. from the S-bend bisection), a line there has no meaningful direction
        val epsilon = 1E-6 * max(1.0, radius)
        val segments = mutableListOf<Segment2D>()
        var cursor = if (closed) features.last().exit else points[0]
        for (feature in features) {
            val gap = cursor.distanceTo(feature.entry)
            if (gap > epsilon) {
                segments.add(Segment2D(cursor, feature.entry))
            } else if (feature.segments.isNotEmpty()) {
                // move the start of the first arc piece, with its control point, onto the cursor
                val first = feature.segments.first()
                val shift = cursor - first.start
                segments.add(Segment2D(cursor, first.control[0] + shift, first.control[1], first.end))
                segments.addAll(feature.segments.drop(1))
                cursor = feature.exit
                continue
            }
            segments.addAll(feature.segments)
            if (feature.segments.isNotEmpty() || gap > epsilon) cursor = feature.exit
        }
        if (!closed) {
            val end = points[n - 1]
            val last = segments.lastOrNull()
            if (cursor.distanceTo(end) > epsilon || last == null || last.linear) {
                if (cursor.distanceTo(end) > 0.0) segments.add(Segment2D(cursor, end))
            } else {
                val shift = end - last.end
                segments[segments.size - 1] = Segment2D(last.start, last.control[0], last.control[1] + shift, end)
            }
        }
        return ShapeContour(segments, closed, polarity)
    }
}
