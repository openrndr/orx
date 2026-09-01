package org.openrndr.extra.shapes.extrema

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.roundToInt

data class LocalMaximum(
    val t: Double,        // ut on the ORIGINAL (non-rectified) contour
    val value: Double,    // f(t)
    val position: Vector2
)

/**
 * Finds local maxima of [f] sampled along [RectifiedContour], using arc-length-uniform
 * sampling so that [windowSize] (expressed as a fraction of curve length) means
 * the same thing everywhere on the contour, regardless of how unevenly the
 * original contour's segments are distributed.
 *
 * @param samples number of evenly arc-length-spaced samples to scan
 * @param windowSize the size of the local neighborhood used to decide whether a
 *                    sample is a maximum, as a fraction of the total curve
 *                    length (e.g. 0.05 = 5% of the curve's length on each side...
 *                    actually total window width, see below)
 * @param f a function evaluated on the ORIGINAL ShapeContour at a given ut,
 *          e.g. `ShapeContour::curvature` or a custom metric
 */
fun RectifiedContour.findLocalMaxima(
    samples: Int,
    windowSize: Double,
    f: ShapeContour.(Double) -> Double
): List<LocalMaximum> {
    require(samples > 2) { "samples must be > 2" }
    require(windowSize > 0.0) { "windowSize must be > 0" }

    val closed = contour.closed

    // Evenly arc-length-spaced sample indices. For closed contours, don't
    // duplicate a sample at both u=0.0 and u=1.0 (they're the same point).
    val sampleCount = if (closed) samples else samples + 1
    val us = List(sampleCount) { i -> i.toDouble() / samples }

    // Convert each uniform-arc-length "u" into the ORIGINAL contour's ut,
    // then evaluate f there.
    val ts = us.map { u -> rectify(u) }
    val values = ts.map { t -> contour.f(t) }

    // windowSize is a fraction of total length -> convert to a sample radius.
    // windowSize * samples = window width in samples; half of that on each side.
    val radius = (windowSize * samples / 2.0).roundToInt().coerceAtLeast(1)

    fun neighborValue(center: Int, offset: Int): Double? {
        val idx = center + offset
        return when {
            closed -> values[((idx % sampleCount) + sampleCount) % sampleCount]
            idx in values.indices -> values[idx]
            else -> null // open contour: no wraparound past the ends
        }
    }

    val maxima = mutableListOf<LocalMaximum>()
    var i = 0
    while (i < sampleCount) {
        val v = values[i]

        // Is v the maximum within [-radius, +radius] around i?
        var isMax = true
        for (offset in -radius..radius) {
            if (offset == 0) continue
            val other = neighborValue(i, offset) ?: continue
            if (other > v) {
                isMax = false
                break
            }
        }

        if (isMax) {
            // Handle flat plateaus (several equal neighboring samples): find
            // the extent of the plateau and take its center, then skip past it.
            var end = i
            while (end + 1 < sampleCount && values[end + 1] == v &&
                (end + 1 - i) <= radius * 2
            ) {
                end++
            }
            val center = (i + end) / 2
            maxima.add(LocalMaximum(ts[center], values[center], position(us[center])))
            i = end + 1
        } else {
            i++
        }
    }

    return maxima
}