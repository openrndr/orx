package org.openrndr.extra.shapes.ph

import org.openrndr.extra.math.complex.Complex
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.atan2

internal fun Vector2.toComplex() = Complex(x, y)
internal fun Complex.toVector2() = Vector2(real, imaginary)

/**
 * A planar Pythagorean-hodograph (PH) quintic: a quintic Bézier curve whose hodograph, its derivative, is the square
 * of a complex quadratic, r'(t) = w(t)², with w(t) = w0·(1-t)² + w1·2t(1-t) + w2·t². Its parametric speed |r'(t)| =
 * |w(t)|² is therefore a polynomial, so unlike for ordinary Béziers the arc length is a polynomial too, the unit tangent
 * and normal are rational, and so are its offset curves. See Farouki, "Pythagorean-Hodograph Curves: Algebra and
 * Geometry Inseparable" (Springer, 2008).
 *
 * Positive offsets lie on the side of the normal (-y, x) of the direction of travel, which is to the right in a y-down
 * frame, as for [org.openrndr.extra.shapes.arcer.arcer].
 */
class PHQuintic internal constructor(
    /** The start point. */
    val start: Vector2,
    internal val w0: Complex,
    internal val w1: Complex,
    internal val w2: Complex
) {
    /** The six control points of the quintic Bézier. */
    val controlPoints: List<Vector2> = run {
        val p1 = start + (w0 * w0).toVector2() / 5.0
        val p2 = p1 + (w0 * w1).toVector2() / 5.0
        val p3 = p2 + (w1 * w1 * 2.0 + w0 * w2).toVector2() / 15.0
        val p4 = p3 + (w1 * w2).toVector2() / 5.0
        val p5 = p4 + (w2 * w2).toVector2() / 5.0
        listOf(start, p1, p2, p3, p4, p5)
    }

    /** The end point. */
    val end: Vector2 get() = controlPoints[5]

    /** Bernstein coefficients of the quartic speed |w(t)|². */
    private val speedCoefficients = doubleArrayOf(
        w0.sqrMagnitude(),
        (w0 * w1.conjugate()).real,
        (w1.sqrMagnitude() * 2.0 + (w0 * w2.conjugate()).real) / 3.0,
        (w1 * w2.conjugate()).real,
        w2.sqrMagnitude()
    )

    /** Bernstein coefficients of the quintic arc length, the integral of the speed. */
    private val lengthCoefficients = DoubleArray(6).also {
        for (k in 1..5) it[k] = it[k - 1] + speedCoefficients[k - 1] / 5.0
    }

    /** The exact arc length. */
    val length: Double get() = lengthCoefficients[5]

    private fun w(t: Double): Complex {
        val u = 1.0 - t
        return w0 * (u * u) + w1 * (2.0 * t * u) + w2 * (t * t)
    }

    private fun dw(t: Double): Complex = (w1 - w0) * (2.0 * (1.0 - t)) + (w2 - w1) * (2.0 * t)

    /** The position at parameter [t] in [0, 1]. */
    fun position(t: Double): Vector2 = bernstein(controlPoints, t)

    /** The derivative r'(t) = w(t)². */
    fun derivative(t: Double): Vector2 = w(t).let { it * it }.toVector2()

    /** The parametric speed |r'(t)| = |w(t)|², a polynomial. */
    fun speed(t: Double): Double = w(t).sqrMagnitude()

    /** The exact arc length from the start to parameter [t]. */
    fun arcLength(t: Double): Double = bernstein(lengthCoefficients, t)

    /** The parameter at which the arc length from the start equals [length], by Newton's method on the polynomial. */
    fun parameterAtLength(length: Double): Double {
        if (length <= 0.0) return 0.0
        if (length >= this.length) return 1.0
        var lo = 0.0
        var hi = 1.0
        var t = length / this.length
        repeat(50) {
            val f = arcLength(t) - length
            if (f > 0.0) hi = t else lo = t
            val s = speed(t)
            var next = if (s > 0.0) t - f / s else Double.NaN
            // the arc length is monotone; fall back to bisection when Newton leaves the bracket
            if (!(next > lo && next < hi)) next = (lo + hi) / 2.0
            if (abs(next - t) < 1E-15) return next
            t = next
        }
        return t
    }

    /** The unit tangent w²/|w|², rational in [t]. */
    fun tangent(t: Double): Vector2 = w(t).let { (it * it).toVector2() / it.sqrMagnitude() }

    /** The unit normal (-y, x) of the [tangent]. */
    fun normal(t: Double): Vector2 = tangent(t).let { Vector2(-it.y, it.x) }

    /** The signed curvature 2·Im(conj(w)·w') / |w|⁴, positive where the curve turns towards the [normal]. */
    fun curvature(t: Double): Double {
        val w = w(t)
        val s = w.sqrMagnitude()
        return 2.0 * (w.conjugate() * dw(t)).imaginary / (s * s)
    }

    /** The exact point of the offset curve at distance [distance] along the [normal]. */
    fun offset(t: Double, distance: Double): Vector2 = position(t) + normal(t) * distance

    /** The derivative of the offset curve, r'(t)·(1 - distance·κ(t)). */
    fun offsetDerivative(t: Double, distance: Double): Vector2 = derivative(t) * (1.0 - distance * curvature(t))

    /**
     * Cubic Bézier segments that approximate the curve, or its offset at [offset], within [tolerance]. Every piece
     * matches the position and derivative exactly at both ends, pieces are halved until their midpoint deviates less
     * than [tolerance].
     */
    fun segments(offset: Double = 0.0, tolerance: Double = 0.01): List<Segment2D> =
        if (offset == 0.0) cubicApproximation(::position, ::derivative, tolerance)
        else cubicApproximation({ offset(it, offset) }, { offsetDerivative(it, offset) }, tolerance)

    /** The curve, or its offset at [offset], as a contour of cubic Béziers within [tolerance], see [segments]. */
    fun contour(
        offset: Double = 0.0,
        tolerance: Double = 0.01,
        polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
    ): ShapeContour = ShapeContour.fromSegments(segments(offset, tolerance), false, polarity)

    /** The absolute rotation of the tangent, the integral of |κ| ds, sampled. */
    internal fun absoluteRotation(samples: Int = 256): Double {
        var total = 0.0
        var previous = derivative(0.0)
        for (k in 1..samples) {
            val d = derivative(k / samples.toDouble())
            if (d.squaredLength > 0.0 && previous.squaredLength > 0.0) {
                total += abs(atan2(previous.x * d.y - previous.y * d.x, previous.dot(d)))
            }
            previous = d
        }
        return total
    }
}

/** de Casteljau evaluation of a Bézier with control points [points] at [t]. */
private fun bernstein(points: List<Vector2>, t: Double): Vector2 {
    val p = points.toMutableList()
    for (r in 1 until p.size) for (i in 0 until p.size - r) p[i] = p[i] * (1.0 - t) + p[i + 1] * t
    return p[0]
}

private fun bernstein(coefficients: DoubleArray, t: Double): Double {
    val c = coefficients.copyOf()
    for (r in 1 until c.size) for (i in 0 until c.size - r) c[i] = c[i] * (1.0 - t) + c[i + 1] * t
    return c[0]
}

/**
 * Cubic Hermite pieces through [position] with [derivative] exact at their ends, on the parameter interval [0, 1],
 * halved until within [tolerance].
 */
internal fun cubicApproximation(
    position: (Double) -> Vector2,
    derivative: (Double) -> Vector2,
    tolerance: Double
): List<Segment2D> {
    val result = mutableListOf<Segment2D>()
    fun piece(a: Double, b: Double, pa: Vector2, pb: Vector2, depth: Int) {
        val h = b - a
        val segment = Segment2D(pa, pa + derivative(a) * (h / 3.0), pb - derivative(b) * (h / 3.0), pb)
        val m = (a + b) / 2.0
        val pm = position(m)
        val error = maxOf(
            segment.position(0.5).distanceTo(pm),
            segment.position(0.25).distanceTo(position(a + h / 4.0)),
            segment.position(0.75).distanceTo(position(a + 3.0 * h / 4.0))
        )
        if (error <= tolerance || depth >= 16) {
            result.add(segment)
        } else {
            piece(a, m, pa, pm, depth + 1)
            piece(m, b, pm, pb, depth + 1)
        }
    }
    // start from two pieces so that a symmetric error can't hide at the midpoint
    val p0 = position(0.0)
    val pm = position(0.5)
    val p1 = position(1.0)
    piece(0.0, 0.5, p0, pm, 1)
    piece(0.5, 1.0, pm, p1, 1)
    return result
}

/**
 * The PH quintic from [start] to [end] with derivatives [startDerivative] and [endDerivative] at its ends, after Farouki
 * and Neff, "Hermite interpolation by Pythagorean hodograph quintics" (Mathematics of Computation, 1995). With w0 =
 * √d0 and w2 = ±√d1 the end condition ∫w² = end - start is a quadratic in w1,
 * 2·w1² + 3(w0 + w2)·w1 + 3w0² + w0w2 + 3w2² - 15(end - start) = 0,
 * so there are four interpolants. The one with the smallest absolute rotation of its tangent is returned, which is the
 * one without loops for reasonable data.
 */
fun phQuinticHermite(start: Vector2, startDerivative: Vector2, end: Vector2, endDerivative: Vector2): PHQuintic =
    phQuinticHermiteCandidates(start, startDerivative, end, endDerivative).minBy { it.absoluteRotation() }

internal fun phQuinticHermiteCandidates(
    start: Vector2,
    startDerivative: Vector2,
    end: Vector2,
    endDerivative: Vector2
): List<PHQuintic> {
    val d0 = startDerivative.toComplex()
    val d1 = endDerivative.toComplex()
    val dp = (end - start).toComplex()
    val w0 = d0.sqrt()
    return listOf(1.0, -1.0).flatMap { s2 ->
        val w2 = d1.sqrt() * s2
        val root = (dp * 120.0 - (d0 + d1) * 15.0 + w0 * w2 * 10.0).sqrt()
        listOf(1.0, -1.0).map { s1 ->
            val w1 = ((w0 + w2) * -3.0 + root * s1) / 4.0
            PHQuintic(start, w0, w1, w2)
        }
    }
}
