package org.openrndr.extra.textoncontour

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform

/**
 * The baseline for text on a [contour]: the curve at distance [offset] from the contour along its normal, the
 * direction of the ascenders of the text. Positions along the baseline are normalized by its [length], so glyph
 * spacing is correct on the offset curve, which is longer than the contour on the outside of a bend and shorter on
 * the inside.
 *
 * @param samples the number of samples used to measure the offset curve
 */
internal class BaselinePath(private val contour: RectifiedContour, val offset: Double, samples: Int = 1024) {
    private val closed = contour.contour.closed

    // cumulative length of the offset curve at contour parameters i / samples, null when there is no offset
    private val lengths: DoubleArray?

    /** The length of the baseline */
    val length: Double

    init {
        if (offset == 0.0 || contour.contour.empty) {
            lengths = null
            length = contour.contour.length
        } else {
            lengths = DoubleArray(samples + 1)
            var previous = point(0.0)
            for (i in 1..samples) {
                val current = point(i.toDouble() / samples)
                lengths[i] = lengths[i - 1] + current.distanceTo(previous)
                previous = current
            }
            length = lengths[samples]
        }
    }

    private fun point(t: Double): Vector2 = contour.position(t) + contour.normal(t) * offset

    /**
     * The contour parameter for the normalized position [u] along the baseline, wraps around for closed contours.
     */
    fun t(u: Double): Double {
        val v = if (closed) u.mod(1.0) else u.coerceIn(0.0, 1.0)
        val lengths = lengths ?: return v
        val target = v * length
        var low = 0
        var high = lengths.size - 1
        while (high - low > 1) {
            val middle = (low + high) / 2
            if (lengths[middle] <= target) low = middle else high = middle
        }
        val span = lengths[high] - lengths[low]
        val fraction = if (span > 0.0) (target - lengths[low]) / span else 0.0
        return (low + fraction) / (lengths.size - 1)
    }

    /**
     * The pose at the normalized position [u] along the baseline: the contour's pose moved along its normal by
     * [offset].
     */
    fun pose(u: Double): Matrix44 {
        val pose = contour.pose(t(u))
        return if (offset == 0.0) pose else pose * transform { translate(0.0, offset) }
    }
}
