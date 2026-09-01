package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Distorts the contour underlying this [RectifiedContour] using a distortion function that is
 * evaluated on the rectified (proportional to contour length) t parameter.
 *
 * This works like [ShapeContour.distort], but the [distort] function additionally receives the
 * rectified t value of the point it is displacing. That makes it possible to express distortions
 * in terms of arc length position, for example tapering an effect towards the ends of the contour
 * or applying a wave with a constant wavelength in curve-length units.
 *
 * The result is built from adaptively split cubic Beziers, one group per segment of the original
 * contour, such that corners of the original contour are preserved. Each candidate piece is fit
 * with its endpoints fixed to the true distorted positions at that piece's boundaries, so
 * neighbouring pieces always join up exactly.
 *
 * @param errorTolerance max allowed RMS fit error before a piece is split in two
 * @param maxDepth recursion limit; caps output at 2^maxDepth pieces per segment. Keep this modest (5-8)
 * @param samplesPerPiece interior samples used both for fitting and for measuring the resulting error
 * @param distort maps a rectified t value and the position at that t to a distorted position
 */
fun RectifiedContour.distort(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    distort: (t: Double, position: Vector2) -> Vector2
): ShapeContour {
    if (contour.empty) {
        return ShapeContour.EMPTY
    }

    fun displaced(t: Double): Vector2 = distort(t, position(t))

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /**
     * Fits a cubic Bezier to displaced(t) for rectified t in [t0, t1] and returns the fitted
     * segment together with the RMS error against the sampled field.
     */
    fun fitRange(t0: Double, t1: Double): Pair<Segment2D, Double> {
        val q0 = displaced(t0)
        val q3 = displaced(t1)

        var sB1B1 = 0.0; var sB1B2 = 0.0; var sB2B2 = 0.0
        var rhs1 = Vector2.ZERO; var rhs2 = Vector2.ZERO
        val localSamples = ArrayList<Pair<Double, Vector2>>(samplesPerPiece - 1)

        for (i in 1 until samplesPerPiece) {
            val s = i.toDouble() / samplesPerPiece
            val t = t0 + (t1 - t0) * s
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

    val segmentCount = contour.segments.size
    val rectifiedBounds = (0..segmentCount).map { inverseRectify(it.toDouble() / segmentCount) }

    val segments = rectifiedBounds.zipWithNext().filter { (t0, t1) -> t1 > t0 }.flatMap { (t0, t1) ->
        recurse(t0, t1, 0)
    }

    return if (segments.isEmpty()) {
        ShapeContour.EMPTY
    } else {
        ShapeContour(segments, contour.closed)
    }
}

/**
 * Distorts the contour underlying this [RectifiedContour] using a distortion function that is
 * evaluated on the rectified (proportional to contour length) t parameter.
 *
 * This works like [ShapeContour.distort], but the [distort] function additionally receives the
 * rectified t value of the point it is displacing. That makes it possible to express distortions
 * in terms of arc length position, for example tapering an effect towards the ends of the contour
 * or applying a wave with a constant wavelength in curve-length units.
 *
 * The result is built from adaptively split cubic Beziers, one group per segment of the original
 * contour, such that corners of the original contour are preserved. Each candidate piece is fit
 * with its endpoints fixed to the true distorted positions at that piece's boundaries, so
 * neighbouring pieces always join up exactly.
 *
 * @param errorTolerance max allowed RMS fit error before a piece is split in two
 * @param maxDepth recursion limit; caps output at 2^maxDepth pieces per segment. Keep this modest (5-8)
 * @param samplesPerPiece interior samples used both for fitting and for measuring the resulting error
 * @param distort maps a rectified t value and the position at that t to a distorted position
 */
fun RectifiedContour.distortUniform(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    sampleDistance: Double = 4.0,
    distort: (t: Double, position: Vector2) -> Vector2
): ShapeContour {
    if (contour.empty) {
        return ShapeContour.EMPTY
    }

    fun displaced(t: Double): Vector2 = distort(t, position(t))

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /**
     * Fits a cubic Bezier to displaced(t) for rectified t in [t0, t1] and returns the fitted
     * segment together with the RMS error against the sampled field.
     */
    fun fitRange(t0: Double, t1: Double): Pair<Segment2D, Double> {
        val q0 = displaced(t0)
        val q3 = displaced(t1)

        var sB1B1 = 0.0; var sB1B2 = 0.0; var sB2B2 = 0.0
        var rhs1 = Vector2.ZERO; var rhs2 = Vector2.ZERO
        val sampleCount = (((t1 - t0)*contour.length) / sampleDistance).toInt().coerceAtLeast(2)

        val localSamples = ArrayList<Pair<Double, Vector2>>(sampleCount - 1)

        for (i in 1 until sampleCount) {
            val s = i.toDouble() / sampleCount
            val t = t0 + (t1 - t0) * s
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

    val segmentCount = contour.segments.size
    val rectifiedBounds = (0..segmentCount).map { inverseRectify(it.toDouble() / segmentCount) }

    val segments = rectifiedBounds.zipWithNext().filter { (t0, t1) -> t1 > t0 }.flatMap { (t0, t1) ->
        recurse(t0, t1, 0)
    }

    return if (segments.isEmpty()) {
        ShapeContour.EMPTY
    } else {
        ShapeContour(segments, contour.closed)
    }
}
