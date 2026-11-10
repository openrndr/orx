package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Distorts the path underlying this [RectifiedPath3D] using a distortion function that is evaluated on the rectified
 * (proportional to path length) t parameter: the 3D counterpart of
 * [org.openrndr.extra.shapes.rectify.RectifiedContour.distort].
 *
 * This works like [Path3D.distort], but the [distort] function additionally receives the rectified t value of the
 * point it is displacing. That makes it possible to express distortions in terms of arc length position, for example
 * tapering an effect towards the ends of the path or applying a wave with a constant wavelength in path length units.
 *
 * The result is built from adaptively split cubic Beziers, one group per segment of the original path, such that
 * corners of the original path are preserved. Each candidate piece is fit with its endpoints fixed to the true
 * distorted positions at that piece's boundaries, so neighbouring pieces always join up exactly.
 *
 * @param errorTolerance max allowed RMS fit error before a piece is split in two
 * @param maxDepth recursion limit; caps output at 2^maxDepth pieces per segment. Keep this modest (5-8)
 * @param samplesPerPiece interior samples used both for fitting and for measuring the resulting error
 * @param distort maps a rectified t value and the position at that t to a distorted position
 */
fun RectifiedPath3D.distort(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    distort: (t: Double, position: Vector3) -> Vector3
): Path3D {
    if (path.empty) {
        return Path3D(emptyList(), path.closed)
    }

    fun displaced(t: Double): Vector3 = distort(t, position(t))

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /**
     * Fits a cubic Bezier to displaced(t) for rectified t in [t0, t1] and returns the fitted segment together with the
     * RMS error against the sampled field.
     */
    fun fitRange(t0: Double, t1: Double): Pair<Segment3D, Double> {
        val q0 = displaced(t0)
        val q3 = displaced(t1)

        var sB1B1 = 0.0; var sB1B2 = 0.0; var sB2B2 = 0.0
        var rhs1 = Vector3.ZERO; var rhs2 = Vector3.ZERO
        val localSamples = ArrayList<Pair<Double, Vector3>>(samplesPerPiece - 1)

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

        val fitted = Segment3D(q0, c0, c1, q3)

        var sqErr = 0.0
        for ((s, d) in localSamples) {
            val diff = d - fitted.position(s)
            sqErr += diff.length * diff.length
        }
        val rms = if (localSamples.isEmpty()) 0.0 else sqrt(sqErr / localSamples.size)

        return fitted to rms
    }

    fun recurse(t0: Double, t1: Double, depth: Int): List<Segment3D> {
        val (segment, rms) = fitRange(t0, t1)
        return if (rms <= errorTolerance || depth >= maxDepth) {
            listOf(segment)
        } else {
            val tm = (t0 + t1) / 2.0
            recurse(t0, tm, depth + 1) + recurse(tm, t1, depth + 1)
        }
    }

    val segmentCount = path.segments.size
    val rectifiedBounds = (0..segmentCount).map { inverseRectify(it.toDouble() / segmentCount) }

    val segments = rectifiedBounds.zipWithNext().filter { (t0, t1) -> t1 > t0 }.flatMap { (t0, t1) ->
        recurse(t0, t1, 0)
    }

    return Path3D(segments, path.closed)
}

/**
 * Distorts the path underlying this [RectifiedPath3D] using a distortion function that is evaluated on the rectified
 * (proportional to path length) t parameter, with samples at equal distances along the path: the 3D counterpart of
 * [org.openrndr.extra.shapes.rectify.RectifiedContour.distortUniform].
 *
 * Unlike [distort], the number of samples of a piece follows its length, one every [sampleDistance], and the control
 * points of every piece are constrained to the directions of the distorted path at its ends, so that pieces join
 * smoothly where the distorted path is smooth and short pieces don't overshoot their ends.
 *
 * Where the distorted path runs backwards relative to the original one it folds over itself; the folds are kept. The
 * trimming of [org.openrndr.extra.shapes.rectify.RectifiedContour.distortUniform] joins the sides of a fold where they
 * cross, which in 3D they generally don't.
 *
 * @param errorTolerance max allowed RMS fit error before a piece is split in two
 * @param maxDepth recursion limit; caps output at 2^maxDepth pieces per segment. Keep this modest (5-8)
 * @param sampleDistance distance in path length between samples used for fitting and measuring the error
 * @param distort maps a rectified t value and the position at that t to a distorted position
 */
fun RectifiedPath3D.distortUniform(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    sampleDistance: Double = 4.0,
    distort: (t: Double, position: Vector3) -> Vector3
): Path3D {
    if (path.empty) {
        return Path3D(emptyList(), path.closed)
    }

    fun displaced(t: Double): Vector3 = distort(t, position(t))

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /**
     * Fits a cubic Bezier to displaced(t) for rectified t in [t0, t1] and returns the fitted segment together with the
     * RMS error against the sampled field.
     */
    fun fitRange(t0: Double, t1: Double): Pair<Segment3D, Double> {
        val q0 = displaced(t0)
        val q3 = displaced(t1)

        // The control points are constrained to the tangent directions of the distorted path at both ends, only their
        // distances along the tangents are fitted. Free control points can overshoot the ends of the piece, which leaves
        // tiny loops at junctions with neighbouring pieces, most notably at short pieces.
        val e = (t1 - t0) * 1E-3
        val chord = (q3 - q0).length
        val tangent0 = (displaced(t0 + e) - displaced(t0)).let { if (it.length > 1E-12) it.normalized else (q3 - q0).normalized }
        val tangent1 = (displaced(t1) - displaced(t1 - e)).let { if (it.length > 1E-12) it.normalized else (q3 - q0).normalized }

        var m11 = 0.0; var m12 = 0.0; var m22 = 0.0
        var r1 = 0.0; var r2 = 0.0
        // the fit must be overdetermined, with too few samples it interpolates them exactly and its error says nothing
        val sampleCount = (((t1 - t0) * path.length) / sampleDistance).toInt().coerceAtLeast(8)

        val localSamples = ArrayList<Pair<Double, Vector3>>(sampleCount - 1)

        for (i in 1 until sampleCount) {
            val s = i.toDouble() / sampleCount
            val t = t0 + (t1 - t0) * s
            val d = displaced(t)
            localSamples.add(s to d)

            val b1s = b1(s); val b2s = b2(s)
            val a1 = tangent0 * b1s
            val a2 = tangent1 * -b2s
            val residual = d - q0 * ((1.0 - s).pow(3) + b1s) - q3 * (b2s + s.pow(3))

            m11 += a1.dot(a1)
            m12 += a1.dot(a2)
            m22 += a2.dot(a2)
            r1 += a1.dot(residual)
            r2 += a2.dot(residual)
        }

        val det = m11 * m22 - m12 * m12
        var alpha = if (abs(det) < 1e-12) -1.0 else (r1 * m22 - r2 * m12) / det
        var beta = if (abs(det) < 1e-12) -1.0 else (m11 * r2 - m12 * r1) / det
        // negative distances put a control point behind its end, distances adding up to more than the chord let the
        // curve run past its end and double back
        if (alpha < 1E-6 * chord || beta < 1E-6 * chord || alpha + beta > chord) {
            alpha = chord / 3.0
            beta = chord / 3.0
        }
        val c0 = q0 + tangent0 * alpha
        val c1 = q3 - tangent1 * beta

        val fitted = Segment3D(q0, c0, c1, q3)

        var sqErr = 0.0
        for ((s, d) in localSamples) {
            val diff = d - fitted.position(s)
            sqErr += diff.length * diff.length
        }
        val rms = if (localSamples.isEmpty()) 0.0 else sqrt(sqErr / localSamples.size)

        return fitted to rms
    }

    fun recurse(t0: Double, t1: Double, depth: Int): List<Segment3D> {
        val (segment, rms) = fitRange(t0, t1)
        return if (rms <= errorTolerance || depth >= maxDepth) {
            listOf(segment)
        } else {
            val tm = (t0 + t1) / 2.0
            recurse(t0, tm, depth + 1) + recurse(tm, t1, depth + 1)
        }
    }

    val segmentCount = path.segments.size
    val rectifiedBounds = (0..segmentCount).map { inverseRectify(it.toDouble() / segmentCount) }

    val segments = rectifiedBounds.zipWithNext().filter { (t0, t1) -> t1 > t0 }.flatMap { (t0, t1) ->
        recurse(t0, t1, 0)
    }

    return Path3D(segments, path.closed)
}
