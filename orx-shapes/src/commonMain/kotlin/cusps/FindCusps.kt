package org.openrndr.extra.shapes.cusps

import org.openrndr.math.Vector2
import org.openrndr.math.solveCubic
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour

/**
 * Finds cusps in [this] contour: points where a single Bezier segment's own
 * parametric speed drops to (near) zero and the direction of travel reverses
 * or becomes undefined - a genuine fold in the curve. This is distinct from
 * an ordinary sharp corner *between* two segments, which has perfectly
 * well-defined (merely discontinuous) tangents on either side and is not
 * reported here.
 *
 * A cusp can only occur *inside* a segment: a cubic segment's velocity
 * `B'(t)` is itself a quadratic-in-t vector (its "hodograph"), and a cusp is
 * exactly where that hodograph passes through - or very near - the origin.
 * Finding it is then a matter of locating the critical points of the
 * speed-squared function `|B'(t)|^2`; since its derivative
 * `2 B'(t) . B''(t)` is an exact cubic polynomial in t (quadratic segments
 * degrade this to a linear one, linear segments have constant, never-zero
 * speed and are skipped), every candidate can be found directly via
 * [solveCubic] rather than by sampling - so a cusp confined to a tiny
 * fraction of a segment's domain can't be missed between samples.
 *
 * @param relativeSpeedThreshold a candidate point is accepted as a cusp when
 * the segment's speed there drops below this fraction of the segment's own
 * length - a relative test, so it scales with the contour's own units
 * instead of requiring an absolute distance.
 * @return the global contour parameters (in the same `ut` space as
 * [ShapeContour.position]) of every detected cusp, ascending and with
 * near-duplicates (e.g. a cusp landing on a shared vertex) merged.
 */
fun ShapeContour.findCusps(relativeSpeedThreshold: Double = 1e-3): List<Double> {
    if (segments.isEmpty()) return emptyList()
    val segmentCount = segments.size
    val found = mutableListOf<Double>()

    for ((index, segment) in segments.withIndex()) {
        if (segment.linear) continue

        val (a, b, c) = segment.hodographCoefficients()

        // d/dt[|B'(t)|^2] = 2 B'(t) . B''(t), with B'(t) = a*t^2 + b*t + c and
        // B''(t) = 2*a*t + b; expanding the dot product gives this cubic.
        val p3 = 2.0 * a.dot(a)
        val p2 = 3.0 * a.dot(b)
        val p1 = b.dot(b) + 2.0 * a.dot(c)
        val p0 = b.dot(c)

        val speedScale = segment.length.coerceAtLeast(1e-9)
        for (root in solveCubic(p3, p2, p1, p0)) {
            if (root < -1e-9 || root > 1.0 + 1e-9) continue
            val t = root.coerceIn(0.0, 1.0)
            val speed = segment.derivative(t).length
            if (speed < relativeSpeedThreshold * speedScale) {
                found.add((index + t) / segmentCount)
            }
        }
    }

    found.sort()
    val merged = mutableListOf<Double>()
    for (t in found) {
        if (merged.isEmpty() || t - merged.last() > 1e-6) {
            merged.add(t)
        }
    }
    return merged
}

/**
 * The coefficients (a, b, c) such that this segment's derivative is
 * `B'(t) = a*t^2 + b*t + c`. Quadratic segments come back with a zero leading
 * term so callers can treat every segment uniformly.
 */
private fun Segment2D.hodographCoefficients(): Triple<Vector2, Vector2, Vector2> = when (control.size) {
    1 -> {
        val p0 = start; val p1 = control[0]; val p2 = end
        val c = (p1 - p0) * 2.0
        val b = (p0 - p1 * 2.0 + p2) * 2.0
        Triple(Vector2.ZERO, b, c)
    }

    2 -> {
        val p0 = start; val p1 = control[0]; val p2 = control[1]; val p3 = end
        val c = (p1 - p0) * 3.0
        val b = (p0 - p1 * 2.0 + p2) * 6.0
        val a = (p3 - p2 * 3.0 + p1 * 3.0 - p0) * 3.0
        Triple(a, b, c)
    }

    else -> error("unsupported segment type for cusp detection")
}
