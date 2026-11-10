package org.openrndr.extra.shapes.rectify

import org.openrndr.extra.shapes.utilities.splitAtBase
import org.openrndr.math.EuclideanVector
import org.openrndr.math.clamp
import org.openrndr.shape.Path

/**
 * Provides a rectified representation of a path in N-dimensional Euclidean space. Rectification refers
 * to the process of mapping the parameter space of the path to a uniform distribution based on arc length.
 *
 * @param T The specific type of Euclidean vector representing the dimension of the path.
 * @property originalPath The underlying path being rectified.
 * @property points Final list of points used in the rectification process, possibly including an additional
 * point to close the loop if the original path is closed.
 * @property intervals Lazy-evaluated list of parameter intervals corresponding to the `points` property,
 * used for mapping and inverse mapping of parameter values.
 *
 * @param distanceTolerance The acceptable tolerance for the distance error in the rectification process.
 * Default value is 0.5.
 * @param lengthScale Scale factor to adjust the length of the Look-Up Table (LUT) for the rectified path.
 * Default value is 1.0.
 */
abstract class RectifiedPath<T : EuclideanVector<T>>(
    val originalPath: Path<T>,
    distanceTolerance: Double = 0.5,
    lengthScale: Double = 1.0
) {
    //<editor-fold desc="the table of lengths">
    // the parameters of the path and the lengths along it, at samples of the parameter of every segment: 1 interval
    // for a linear segment, [SAMPLES] for a curved one; in every interval the parameter follows the length as a cubic
    // Hermite curve, with the rate at which the parameter changes with the length at both ends, so that positions at
    // equal steps of the rectified parameter are equally far apart and move at an even pace
    private val tableT: DoubleArray
    private val tableS: DoubleArray

    // dt/ds at the start and at the end of every interval, within its segment
    private val slope0: DoubleArray
    private val slope1: DoubleArray

    init {
        val segments = originalPath.segments
        val m = segments.size
        val ts = ArrayList<Double>()
        val ss = ArrayList<Double>()
        val s0 = ArrayList<Double>()
        val s1 = ArrayList<Double>()
        if (m > 0) {
            var length = 0.0
            ts.add(0.0); ss.add(0.0)
            for ((i, segment) in segments.withIndex()) {
                val samples = if (segment.linear) 1 else SAMPLES
                // the rate of the parameter of the path per length, from the speed of the segment
                fun slope(u: Double): Double {
                    val speed = segment.derivative(u).length * m
                    return if (speed > 1E-12) 1.0 / speed else Double.NaN
                }
                var previous = segment.start
                for (k in 1..samples) {
                    val u = k.toDouble() / samples
                    val p = segment.position(u)
                    length += (p - previous).length
                    previous = p
                    ts.add((i + u) / m)
                    ss.add(length)
                    s0.add(slope((k - 1).toDouble() / samples))
                    s1.add(slope(u))
                }
            }
        }
        tableT = ts.toDoubleArray()
        tableS = ss.toDoubleArray()
        slope0 = s0.toDoubleArray()
        slope1 = s1.toDoubleArray()
    }

    /** the length of the path, as measured by its table */
    private val tableLength: Double get() = if (tableS.isEmpty()) 0.0 else tableS.last()

    /** the parameter of [originalPath] at [s] along it */
    private fun parameterAt(s: Double): Double {
        if (tableS.size < 2) return 0.0
        val total = tableLength
        if (s <= 0.0) return 0.0
        if (s >= total) return 1.0
        val k = intervalOf(tableS, s)
        val h = tableS[k + 1] - tableS[k]
        if (h <= 0.0) return tableT[k]
        val x = (s - tableS[k]) / h
        val t0 = tableT[k]
        val t1 = tableT[k + 1]
        val m0 = slope0[k]
        val m1 = slope1[k]
        // without the speed at an end, like at a cusp, the parameter follows the length linearly
        if (m0.isNaN() || m1.isNaN()) return t0 + (t1 - t0) * x
        val x2 = x * x
        val x3 = x2 * x
        val t = (2 * x3 - 3 * x2 + 1) * t0 + (x3 - 2 * x2 + x) * h * m0 + (-2 * x3 + 3 * x2) * t1 + (x3 - x2) * h * m1
        return t.coerceIn(t0, t1)
    }

    /** the length along [originalPath] at its parameter [t] */
    private fun lengthAt(t: Double): Double {
        if (tableT.size < 2) return 0.0
        if (t <= 0.0) return 0.0
        if (t >= 1.0) return tableLength
        val k = intervalOf(tableT, t)
        // the inverse of the Hermite curve of the interval, by a few steps of Newton from the linear guess
        val dt = tableT[k + 1] - tableT[k]
        if (dt <= 0.0) return tableS[k]
        var s = tableS[k] + (tableS[k + 1] - tableS[k]) * (t - tableT[k]) / dt
        repeat(4) {
            val e = parameterAt(s) - t
            val h = (tableS[k + 1] - tableS[k]) * 1E-6
            val d = (parameterAt(s + h) - parameterAt(s - h)) / (2 * h)
            if (d <= 0.0 || !d.isFinite()) return s
            s = (s - e / d).coerceIn(tableS[k], tableS[k + 1])
        }
        return s
    }
    //</editor-fold>

    /**
     * the points at equal distances along the path with their parameters of [originalPath], about one per unit of
     * length times [lengthScale]; a closed path has the start again at its end, with the parameter 1.0.
     * [distanceTolerance] is no longer used: the distances follow the segments of the path
     */
    val points: List<Pair<T, Double>> = if (originalPath.empty) emptyList() else {
        val count = (originalPath.length * lengthScale).toInt().coerceAtLeast(2)
        val total = tableLength
        if (originalPath.closed) {
            List(count) { i -> parameterAt(total * i / count).let { t -> originalPath.position(t) to t } } +
                    (originalPath.position(0.0) to 1.0)
        } else {
            List(count) { i -> parameterAt(total * i / (count - 1)).let { t -> originalPath.position(t) to t } }
        }
    }

    val intervals by lazy {
        points.zipWithNext().map {
            Pair(it.first.second, it.second.second)
        }
    }

    internal fun safe(t: Double): Double {
        return if (originalPath.closed) {
            t.mod(1.0)
        } else {
            t.clamp(0.0, 1.0)
        }
    }

    /**
     * computes a rectified t-value for [originalPath]: the parameter of the path at [t] times its length along it
     */
    fun rectify(t: Double): Double {
        if (originalPath.empty) {
            return 0.0
        }
        if (t <= 0.0) return 0.0
        if (t >= 1.0) return 1.0
        return parameterAt(t * tableLength)
    }

    /**
     * Computes an inverse rectified t-value for the given normalized `t` parameter: the part of the length of the
     * path up to its parameter [t].
     *
     * @param t A normalized parameter (between 0.0 and 1.0) in the parameter space of [originalPath].
     *          Values outside this range will be clamped to 0.0 or 1.0.
     * @return A normalized parameter (between 0.0 and 1.0) in rectified parameter space.
     *         Returns 0.0 if the original path is empty.
     */
    fun inverseRectify(t: Double): Double {
        if (originalPath.empty) {
            return 0.0
        }
        if (t <= 0.0) return 0.0
        if (t >= 1.0) return 1.0
        val total = tableLength
        return if (total > 0.0) lengthAt(t) / total else t
    }

    fun position(t: Double): T {
        return if (originalPath.empty) {
            originalPath.infinity
        } else {
            originalPath.position(rectify(safe(t)))
        }
    }

    fun direction(t: Double): T {
        return if (originalPath.empty) {
            originalPath.infinity
        } else {
            originalPath.direction(rectify(safe(t)))
        }
    }

    /**
     * Evaluate curvature for [t]
     */
    fun curvature(t: Double): Double {
        return originalPath.curvature(rectify(safe(t)))
    }

    abstract fun sub(t0: Double, t1: Double): Path<T>

    /**
     * Split contour at [ascendingTs]
     * @since orx 0.4.4
     */
    open fun splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<Path<T>> {
        return originalPath.splitAtBase(ascendingTs.map { rectify(it) }, weldEpsilon)
    }
}

/** the samples of the parameter of a curved segment in the table of lengths of a [RectifiedPath] */
private const val SAMPLES = 32

/** the index of the interval of the ascending [values] that holds [x], from 0 to the second to last index */
private fun intervalOf(values: DoubleArray, x: Double): Int {
    var lo = 0
    var hi = values.size - 1
    while (hi - lo > 1) {
        val mid = (lo + hi) ushr 1
        if (values[mid] <= x) lo = mid else hi = mid
    }
    return lo
}
