package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Convolutes the contour underlying this [RectifiedContour] with [kernel].
 *
 * This works by first generating semi-uniform samples along the contour: samples are placed at
 * roughly [sampleDistance] apart, but the start and end of every original contour segment always
 * coincides with a sample, which is why the resulting spacing is only semi-uniform. The supplied
 * [kernel], which must have an odd size (2n+1), is then applied as a centered convolution over
 * these samples (for closed contours the samples wrap around, for open contours the samples at
 * the ends are repeated).
 *
 * The convoluted samples are then used to re-fit segments, using the same adaptive cubic Bezier
 * fitting strategy as [distort] and [distortUniform], one group per segment of the original
 * contour so corners of the original contour are preserved. Sample indices are tracked so it is
 * known which convoluted samples belong to which original segment.
 *
 * @param errorTolerance max allowed RMS fit error before a piece is split in two
 * @param maxDepth recursion limit; caps output at 2^maxDepth pieces per segment. Keep this modest (5-8)
 * @param sampleDistance approximate distance (in contour length units) between generated samples
 * @param kernel convolution kernel, must have an odd size (2n+1) so it can be applied centered
 */
fun RectifiedContour.convolute(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    sampleDistance: Double = 4.0,
    kernel: DoubleArray,
): ShapeContour {
    if (contour.empty) {
        return ShapeContour.EMPTY
    }

    require(kernel.size % 2 == 1) { "kernel size must be odd (2n+1), got ${kernel.size}" }
    val n = kernel.size / 2

    val closed = contour.closed
    val segmentCount = contour.segments.size
    val rectifiedBounds = (0..segmentCount).map { inverseRectify(it.toDouble() / segmentCount) }

    // Generate semi-uniform samples, one list entry per sample, tracking which sample index
    // range belongs to which segment. Segment start/end points coincide with a sample.
    val samplePositions = ArrayList<Vector2>()
    val segmentSampleRanges = ArrayList<IntRange?>()

    for (segIndex in 0 until segmentCount) {
        val t0 = rectifiedBounds[segIndex]
        val t1 = rectifiedBounds[segIndex + 1]
        if (t1 <= t0) {
            segmentSampleRanges.add(null)
            continue
        }
        if (samplePositions.isEmpty()) {
            samplePositions.add(position(t0))
        }
        val startIndex = samplePositions.size - 1
        val sampleCount = (((t1 - t0) * contour.length) / sampleDistance).toInt().coerceAtLeast(2)
        for (i in 1..sampleCount) {
            val s = i.toDouble() / sampleCount
            val t = t0 + (t1 - t0) * s
            samplePositions.add(position(t))
        }
        val endIndex = samplePositions.size - 1
        segmentSampleRanges.add(startIndex..endIndex)
    }

    // For closed contours the last sample coincides with the first one, so it is excluded from
    // the set of unique samples used for the convolution, and indices wrap around it instead.
    val uniqueCount = if (closed) samplePositions.size - 1 else samplePositions.size

    fun uniqueIndex(i: Int): Int {
        return if (closed) {
            i.mod(uniqueCount)
        } else {
            i.coerceIn(0, uniqueCount - 1)
        }
    }

    val convoluted = Array(samplePositions.size) { i ->
        var result = Vector2.ZERO
        for (k in -n..n) {
            result += samplePositions[uniqueIndex(i + k)] * kernel[k + n]
        }
        result
    }

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /**
     * Fits a cubic Bezier to the convoluted samples with indices in [i0, i1] and returns the
     * fitted segment together with the RMS error against the sampled field.
     */
    fun fitRange(i0: Int, i1: Int): Pair<Segment2D, Double> {
        val q0 = convoluted[i0]
        val q3 = convoluted[i1]

        var sB1B1 = 0.0; var sB1B2 = 0.0; var sB2B2 = 0.0
        var rhs1 = Vector2.ZERO; var rhs2 = Vector2.ZERO
        val localSamples = ArrayList<Pair<Double, Vector2>>(i1 - i0 - 1)

        for (i in i0 + 1 until i1) {
            val s = (i - i0).toDouble() / (i1 - i0)
            val d = convoluted[i]
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

    fun recurse(i0: Int, i1: Int, depth: Int): List<Segment2D> {
        if (i1 - i0 <= 1) {
            return listOf(Segment2D(convoluted[i0], convoluted[i1]))
        }
        val (segment, rms) = fitRange(i0, i1)
        return if (rms <= errorTolerance || depth >= maxDepth) {
            listOf(segment)
        } else {
            val im = (i0 + i1) / 2
            recurse(i0, im, depth + 1) + recurse(im, i1, depth + 1)
        }
    }

    val segments = segmentSampleRanges.filterNotNull().flatMap { range -> recurse(range.first, range.last, 0) }

    return if (segments.isEmpty()) {
        ShapeContour.EMPTY
    } else {
        ShapeContour(segments, contour.closed)
    }
}

