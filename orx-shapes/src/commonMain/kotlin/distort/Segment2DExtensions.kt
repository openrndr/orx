package org.openrndr.extra.shapes.distort

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Approximates the curve you'd get by applying an arbitrary point-wise
 * [distort] function to every point of [curve], while representing the
 * result as adaptively-split cubic Beziers.
 *
 * [distort] just maps a position to its displaced position - it knows
 * nothing about Beziers, attraction, or curve parameters. That means this
 * function works for anything: attraction/repulsion toward one or more
 * points, noise-based warping, swirls, lens-style distortions, etc. All the
 * curve-specific logic (fitting, continuity across splits, error-driven
 * subdivision) lives here and is reused as-is.
 *
 * Each candidate piece is fit with its endpoints FIXED to distort(B(t0)) and
 * distort(B(t1)) - the true distorted position at that piece's boundaries -
 * so neighboring pieces always join up exactly, no matter how many times a
 * piece gets split.
 *
 * Note: because [distort] only sees a position, it can't distinguish "this
 * point is near the curve's original t=0 endpoint" from "this point just
 * happens to be near the attractor spatially". If you need that distinction
 * (e.g. partial endpoint pinning based on arc-length position), change the
 * signature to (Vector2, Double) -> Vector2 and pass `t` through from
 * `displaced()` below - everything else stays the same.
 *
 * @param curve           original cubic Bezier
 * @param errorTolerance  max allowed RMS fit error (same units as the curve's
 *                        coordinates) before a piece is split in two
 * @param maxDepth        recursion limit; caps output at 2^maxDepth pieces.
 *                        Keep this modest (5-8) — it's a hard ceiling, not a target.
 * @param samplesPerPiece interior samples used both for fitting and for measuring
 *                        the resulting error
 * @param distort         maps a point on the original curve to its distorted position
 */
fun Segment2D.distort(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    distort: (Vector2) -> Vector2
): List<Segment2D> = this.distort(errorTolerance, maxDepth, samplesPerPiece) { p, _ -> distort(p) }

/**
 * Parameter-aware variant of [distort]: the [distort] function additionally receives the
 * global parameter `t` of the original curve, which makes it possible to express
 * displacement fields that vary along the curve, for example tapering toward the endpoints.
 *
 * @param curve           original cubic Bezier
 * @param errorTolerance  max allowed RMS fit error before a piece is split in two
 * @param maxDepth        recursion limit; caps output at 2^maxDepth pieces
 * @param samplesPerPiece interior samples used both for fitting and for measuring the error
 * @param distort         maps a point on the original curve, plus its global `t`, to its
 *                        distorted position
 */
fun Segment2D.distort(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    distort: (Vector2, Double) -> Vector2
): List<Segment2D> {

    fun displaced(t: Double): Vector2 = distort(position(t), t)

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    // Fits a cubic Bezier to displaced(t) for global t in [t0, t1].
    // Returns the fitted segment and the RMS error against the sampled field.
    fun fitRange(t0: Double, t1: Double): Pair<Segment2D, Double> {
        val q0 = displaced(t0)
        val q3 = displaced(t1)

        var sB1B1 = 0.0; var sB1B2 = 0.0; var sB2B2 = 0.0
        var rhs1 = Vector2.ZERO; var rhs2 = Vector2.ZERO
        val localSamples = ArrayList<Pair<Double, Vector2>>(samplesPerPiece - 1)

        for (i in 1 until samplesPerPiece) {
            val s = i.toDouble() / samplesPerPiece          // local param, 0..1 within this piece
            val t = t0 + (t1 - t0) * s                      // global param, for evaluating the field
            val d = displaced(t)
            localSamples.add(s to d)

            val b1s = b1(s); val b2s = b2(s)
            val endpointContribution = q0 * (1.0 - s).pow(3) + q3 * s.pow(3)
            val residual = d - endpointContribution

            sB1B1 += b1s * b1s
            sB1B2 += b1s * b2s
            sB2B2 += b2s * b2s
            rhs1 += residual * b1s
            rhs2 += residual * b2s
        }

        val det = sB1B1 * sB2B2 - sB1B2 * sB1B2
        val (c0, c1) = if (abs(det) < 1e-9) {
            Pair(q0 + (q3 - q0) * (1.0 / 3.0), q0 + (q3 - q0) * (2.0 / 3.0))
        } else {
            val cc0 = (rhs1 * sB2B2 - rhs2 * sB1B2) * (1.0 / det)
            val cc1 = (rhs2 * sB1B1 - rhs1 * sB1B2) * (1.0 / det)
            Pair(cc0, cc1)
        }

        val fitted = Segment2D(q0, c0, c1, q3)

        var sqErr = 0.0
        for ((s, d) in localSamples) {
            val diff = d - fitted.position(s)
            sqErr += diff.length * diff.length
        }
        val rms = if (localSamples.isEmpty()) 0.0 else sqrt(sqErr / localSamples.size)

        return fitted to rms
    }

    fun recurse(t0: Double, t1: Double, depth: Int): List<Segment2D> {
        val (segment, rms) = fitRange(t0, t1)
        return if (rms <= errorTolerance || depth >= maxDepth) {
            listOf(segment)
        } else {
            val tm = (t0 + t1) / 2.0
            recurse(t0, tm, depth + 1) + recurse(tm, t1, depth + 1)
        }
    }

    return recurse(0.0, 1.0, 0)
}
