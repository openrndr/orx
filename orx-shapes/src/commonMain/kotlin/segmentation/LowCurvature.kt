package org.openrndr.extra.shapes.segmentation

import org.openrndr.shape.ShapeContour
import kotlin.math.abs

/**
 * Segments this ShapeContour into sub-contours whose curvature stays below
 * [maxCurvature]. High-curvature stretches are dropped as "seams" between
 * segments; only the low-curvature stretches are returned.
 *
 * @param maxCurvature curvature threshold — points above this are treated as
 *                      split points. Tune this to your contour's scale.
 * @param samples number of samples used to scan the contour (higher = more
 *                precise split points, but slower)
 * @param smoothingWindow simple moving-average window (in samples) applied to
 *                         the curvature signal before thresholding, to avoid
 *                         over-segmenting on single-sample noise spikes
 * @param minLengthFraction sub-contours shorter than this fraction of the
 *                           total contour length are discarded
 */
fun ShapeContour.segmentByCurvature(
    maxCurvature: Double = 0.05,
    samples: Int = 500,
    smoothingWindow: Int = 3,
    minLengthFraction: Double = 0.01
): List<ShapeContour> {
    require(samples > 1) { "samples must be > 1" }

    val ts = (0..samples).map { it.toDouble() / samples }
    val rawCurvatures = ts.map { t -> abs(curvature(t)) }

    // simple moving average smoothing
    val curvatures = if (smoothingWindow <= 1) rawCurvatures else
        rawCurvatures.indices.map { i ->
            val lo = (i - smoothingWindow / 2).coerceAtLeast(0)
            val hi = (i + smoothingWindow / 2).coerceAtMost(rawCurvatures.size - 1)
            rawCurvatures.subList(lo, hi + 1).average()
        }

    // find contiguous [t0, t1] ranges where curvature <= maxCurvature
    val ranges = mutableListOf<Pair<Double, Double>>()
    var rangeStart: Double? = null

    for (i in ts.indices) {
        val low = curvatures[i] <= maxCurvature
        when {
            low && rangeStart == null -> rangeStart = ts[i]
            !low && rangeStart != null -> {
                ranges.add(rangeStart to ts[i])
                rangeStart = null
            }
        }
    }
    if (rangeStart != null) ranges.add(rangeStart to ts.last())

    // for closed contours, merge a range touching t=1.0 with one touching t=0.0
    if (closed && ranges.size > 1 &&
        ranges.first().first == 0.0 && ranges.last().second == ts.last()
    ) {
        val first = ranges.removeAt(0)
        val last = ranges.removeAt(ranges.size - 1)
        // represent the wrapped range as ending past 1.0, handled below
        ranges.add(last.first to (1.0 + first.second))
    }

    val totalLength = length
    return ranges.mapNotNull { (t0, t1) ->
        val sub = if (t1 <= 1.0) {
            sub(t0, t1)
        } else {
            // wraps across the seam (t=1.0 -> t=0.0); stitch two subs together
            sub(t0, 1.0) + sub(0.0, t1 - 1.0)
        }
        if (sub.length >= totalLength * minLengthFraction) sub else null
    }
}