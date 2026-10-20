package org.openrndr.extra.shapes.clothoid

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

/** Nodes and weights of 8-point Gauss-Legendre quadrature on [-1, 1]. */
private val glNodes = doubleArrayOf(
    -0.9602898564975363, -0.7966664774136267, -0.5255324099163290, -0.1834346424956498,
    0.1834346424956498, 0.5255324099163290, 0.7966664774136267, 0.9602898564975363
)
private val glWeights = doubleArrayOf(
    0.1012285362903763, 0.2223810344533745, 0.3137066458778873, 0.3626837833783620,
    0.3626837833783620, 0.3137066458778873, 0.2223810344533745, 0.1012285362903763
)

/** The integrals ∫₀¹ tᵏ cos(a/2·t² + b·t + c) dt ([x0], [x1], [x2]) and the same with sin ([y0], [y1], [y2]). */
internal class Moments(
    val x0: Double, val x1: Double, val x2: Double,
    val y0: Double, val y1: Double, val y2: Double
)

/**
 * Evaluates [Moments] by composite Gauss-Legendre quadrature, with enough sub-intervals that the phase changes by at
 * most one radian in each. That keeps the error near machine precision.
 */
internal fun moments(a: Double, b: Double, c: Double): Moments {
    val pieces = ceil(abs(a) + abs(b)).toInt().coerceIn(1, 1_000_000)
    val h = 1.0 / pieces
    var x0 = 0.0; var x1 = 0.0; var x2 = 0.0
    var y0 = 0.0; var y1 = 0.0; var y2 = 0.0
    for (p in 0 until pieces) {
        for (k in glNodes.indices) {
            val t = (p + (glNodes[k] + 1.0) / 2.0) * h
            val w = glWeights[k] * h / 2.0
            val phase = (a / 2.0 * t + b) * t + c
            val wc = w * cos(phase)
            val ws = w * sin(phase)
            x0 += wc; x1 += wc * t; x2 += wc * t * t
            y0 += ws; y1 += ws * t; y2 += ws * t * t
        }
    }
    return Moments(x0, x1, x2, y0, y1, y2)
}

/** The multiple of 2π that wraps [angle] to (-π, π]. */
internal fun wrapOffset(angle: Double) = wrap(angle) - angle

/** [angle] wrapped to (-π, π]. */
private fun wrap(angle: Double): Double {
    var a = angle % (2 * PI)
    if (a <= -PI) a += 2 * PI
    if (a > PI) a -= 2 * PI
    return a
}

/**
 * A clothoid (Euler spiral) segment: it starts at [start] in direction [theta] with curvature [kappa], which changes
 * linearly by [dkappa] per unit of arc length along [length].
 */
internal class Clothoid(
    val start: Vector2,
    val theta: Double,
    val kappa: Double,
    val dkappa: Double,
    val length: Double
) {
    val endKappa get() = kappa + dkappa * length

    fun angle(s: Double) = theta + (kappa + dkappa * s / 2.0) * s

    fun position(s: Double): Vector2 {
        if (s == 0.0) return start
        val m = moments(dkappa * s * s, kappa * s, theta)
        return start + Vector2(m.x0, m.y0) * s
    }

    /**
     * Cubic Bézier approximation: pieces that turn by at most π/16, with the end points and end directions on the
     * clothoid and handles of the length that is exact for a circular arc of the same turn.
     */
    fun segments(end: Vector2): List<Segment2D> {
        val turn = max(abs(kappa), abs(endKappa)) * length
        val pieces = ceil(turn / (PI / 16.0)).toInt().coerceIn(1, 10_000)
        val result = mutableListOf<Segment2D>()
        var s0 = 0.0
        var p0 = start
        for (j in 1..pieces) {
            val s1 = length * j / pieces
            val p1 = if (j == pieces) end else position(s1)
            val a0 = angle(s0)
            val a1 = angle(s1)
            val delta = a1 - a0
            val h = s1 - s0
            val handle = if (abs(delta) < 1E-9) h / 3.0 else 4.0 / 3.0 * tan(delta / 4.0) / delta * h
            result.add(
                Segment2D(
                    p0,
                    p0 + Vector2(cos(a0), sin(a0)) * handle,
                    p1 - Vector2(cos(a1), sin(a1)) * handle,
                    p1
                )
            )
            s0 = s1
            p0 = p1
        }
        return result
    }
}

/** The largest angle relative to the chord at either end of a clothoid fitted by [fitClothoidRelative]. */
private const val MAX_RELATIVE_ANGLE = 3 * PI

/** The largest shape parameter A of a clothoid fitted by [fitClothoidRelative], see there. */
private const val MAX_SHAPE = 100.0

/**
 * The clothoid from [start] leaving at angle [theta0] to [end] arriving at angle [theta1], after Bertolazzi and Frego,
 * "G1 fitting with clothoids" (Mathematical Methods in the Applied Sciences, 2015). The angles relative to the chord are
 * wrapped to (-π, π], which picks the solution without extra windings.
 */
internal fun fitClothoid(start: Vector2, theta0: Double, end: Vector2, theta1: Double): Clothoid {
    val d = end - start
    val phi = atan2(d.y, d.x)
    val phi0 = wrap(theta0 - phi)
    val phi1 = wrap(theta1 - phi)
    return fitClothoidRelative(start, end, phi0, phi1)
        ?: error("no clothoid from $start at $theta0 to $end at $theta1")
}

/**
 * The clothoid from [start] to [end] that leaves at angle [phi0] and arrives at angle [phi1] relative to the chord,
 * which may lie outside (-π, π] for clothoids that wind further. With δ = φ1 - φ0 the angle along the normalized
 * clothoid is φ0 + (δ - A)·t + A·t², and A is the root of Y0(2A, δ - A, φ0) = 0, which says the end point lies on the
 * chord. It is found by Newton's method, first from [guess] when given, then from the initial guess of Bertolazzi and
 * Frego, then from simpler guesses. Returns null when none of them converges to a clothoid that moves forwards along
 * the chord, or when the clothoid would wind far more than any useful solution: relative angles beyond
 * [MAX_RELATIVE_ANGLE] or a shape parameter A beyond [MAX_SHAPE]. These limits also bound the cost of the quadrature.
 */
internal fun fitClothoidRelative(start: Vector2, end: Vector2, phi0: Double, phi1: Double, guess: Double? = null): Clothoid? {
    val d = end - start
    val r = d.length
    val phi = atan2(d.y, d.x)
    val delta = phi1 - phi0

    val x = phi0 / PI
    val y = phi1 / PI
    val xy = x * y
    val xx = x * x
    val yy = y * y
    val paper = (phi0 + phi1) * (2.989696028701907 + xy * (0.716228953608281 + xy * -0.458969738821509) +
            (-0.502821153340377 + xy * 0.261062141752652) * (xx + yy) + -0.045854475238709 * (xx * xx + yy * yy))
    val guesses = listOfNotNull(guess, paper, 3.0 * (phi0 + phi1), 0.0)

    if (abs(phi0) > MAX_RELATIVE_ANGLE || abs(phi1) > MAX_RELATIVE_ANGLE) return null
    for (initial in guesses) {
        var a = initial
        for (iteration in 0 until 50) {
            if (!a.isFinite() || abs(a) > MAX_SHAPE) break
            val m = moments(2.0 * a, delta - a, phi0)
            val derivative = m.x2 - m.x1
            if (derivative == 0.0) break
            val step = m.y0 / derivative
            a -= step
            if (abs(step) < 1E-14 * (1.0 + abs(a))) break
        }
        if (!a.isFinite() || abs(a) > MAX_SHAPE) continue
        val m = moments(2.0 * a, delta - a, phi0)
        if (abs(m.y0) > 1E-10 || m.x0 <= 0.0) continue
        val length = r / m.x0
        return Clothoid(start, phi + phi0, (delta - a) / length, 2.0 * a / (length * length), length)
    }
    return null
}
