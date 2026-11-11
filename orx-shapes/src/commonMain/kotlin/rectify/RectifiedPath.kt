package org.openrndr.extra.shapes.rectify

import org.openrndr.extra.shapes.utilities.splitAtBase
import org.openrndr.math.EuclideanVector
import org.openrndr.math.clamp
import org.openrndr.shape.Path
import kotlin.math.abs

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
 * @param distanceTolerance No longer used: the table of lengths follows the segments of the path, see [lengthTolerance].
 * @param lengthScale Scale factor to adjust the number of [points], about one per unit of length times [lengthScale].
 * Default value is 1.0.
 * @param lengthTolerance The largest error, in the units of the path, of the lengths along the path that the
 * rectification works with. Default value is 1E-3.
 */
abstract class RectifiedPath<T : EuclideanVector<T>>(
    val originalPath: Path<T>,
    distanceTolerance: Double = 0.5,
    private val lengthScale: Double = 1.0,
    val lengthTolerance: Double = 1E-3
) {
    //<editor-fold desc="the table of lengths">
    // the parameters of the path and the lengths along it at the ends of intervals of the parameter: 1 interval for a
    // linear segment, and for a curved one intervals that are halved until they fit; in every interval the length
    // follows the parameter as a cubic Hermite curve, with the speed of the path at both ends. The lengths are
    // Gauss-Legendre integrals of the speed, so they agree with the speeds, and the length as a function of the
    // parameter stays smooth where the speed drops to nothing, like at a cusp
    private val tableT: DoubleArray
    private val tableS: DoubleArray

    // ds/dt, the speed of the path by its own parameter, at the start and at the end of every interval
    private val speed0: DoubleArray
    private val speed1: DoubleArray

    init {
        val segments = originalPath.segments
        val m = segments.size
        val ts = ArrayList<Double>()
        val ss = ArrayList<Double>()
        val v0 = ArrayList<Double>()
        val v1 = ArrayList<Double>()
        if (m > 0) {
            var length = 0.0
            ts.add(0.0); ss.add(0.0)
            for ((i, segment) in segments.withIndex()) {
                if (segment.linear) {
                    val l = (segment.end - segment.start).length
                    length += l
                    ts.add((i + 1.0) / m); ss.add(length)
                    v0.add(l * m); v1.add(l * m)
                    continue
                }
                // the speed by the parameter of the path, which runs 1/m along a segment
                fun speed(u: Double): Double = segment.derivative(u).length * m

                // the length of the segment from u0 to u1
                fun integral(u0: Double, u1: Double): Double {
                    val h = (u1 - u0) / 2
                    val mid = (u0 + u1) / 2
                    var sum = 0.0
                    for (k in GAUSS_X.indices) sum += GAUSS_W[k] * speed(mid + h * GAUSS_X[k])
                    return sum * h / m
                }

                // adds the interval from u0 to u1 with [l] its length, or its halves when the Hermite curve misses
                // the length to its middle or the halves add up to another length
                fun add(u0: Double, u1: Double, l: Double, sp0: Double, sp1: Double, depth: Int) {
                    val um = (u0 + u1) / 2
                    val left = integral(u0, um)
                    val right = integral(um, u1)
                    val hermite = l / 2 + (u1 - u0) / m / 8 * (sp0 - sp1)
                    if (depth < MAX_DEPTH &&
                        (abs(hermite - left) > lengthTolerance || abs(left + right - l) > lengthTolerance)
                    ) {
                        val spm = speed(um)
                        add(u0, um, left, sp0, spm, depth + 1)
                        add(um, u1, right, spm, sp1, depth + 1)
                    } else {
                        length += l
                        ts.add((i + u1) / m); ss.add(length)
                        v0.add(sp0); v1.add(sp1)
                    }
                }
                for (k in 0 until INITIAL_INTERVALS) {
                    val u0 = k.toDouble() / INITIAL_INTERVALS
                    val u1 = (k + 1.0) / INITIAL_INTERVALS
                    add(u0, u1, integral(u0, u1), speed(u0), speed(u1), 0)
                }
            }
            // the last parameter exactly 1.0
            ts[ts.lastIndex] = 1.0
        }
        tableT = ts.toDoubleArray()
        tableS = ss.toDoubleArray()
        speed0 = v0.toDoubleArray()
        speed1 = v1.toDoubleArray()
    }

    /** the length of the path, as measured by its table */
    private val tableLength: Double get() = if (tableS.isEmpty()) 0.0 else tableS.last()

    /** the parameter of [originalPath] at [s] along it */
    private fun parameterAt(s: Double): Double {
        if (tableS.size < 2) return 0.0
        if (s <= 0.0) return 0.0
        if (s >= tableLength) return 1.0
        val k = intervalOf(tableS, s)
        val s0 = tableS[k]
        val s1 = tableS[k + 1]
        val h = tableT[k + 1] - tableT[k]
        if (s1 <= s0) return tableT[k]
        val m0 = h * speed0[k]
        val m1 = h * speed1[k]
        // the Hermite curve of the interval solved for s by Newton steps, kept within the bracket of the solution
        var lo = 0.0
        var hi = 1.0
        var x = (s - s0) / (s1 - s0)
        val epsilon = (s1 - s0) * 1E-13
        for (iteration in 0 until 32) {
            val x2 = x * x
            val x3 = x2 * x
            val f = (2 * x3 - 3 * x2 + 1) * s0 + (x3 - 2 * x2 + x) * m0 + (-2 * x3 + 3 * x2) * s1 + (x3 - x2) * m1 - s
            if (abs(f) <= epsilon) break
            if (f > 0.0) hi = x else lo = x
            val d = (6 * x2 - 6 * x) * (s0 - s1) + (3 * x2 - 4 * x + 1) * m0 + (3 * x2 - 2 * x) * m1
            val next = x - f / d
            x = if (next > lo && next < hi) next else (lo + hi) / 2
            if (hi - lo < 1E-15) break
        }
        return tableT[k] + h * x
    }

    /** the length along [originalPath] at its parameter [t] */
    private fun lengthAt(t: Double): Double {
        if (tableT.size < 2) return 0.0
        if (t <= 0.0) return 0.0
        if (t >= 1.0) return tableLength
        val k = intervalOf(tableT, t)
        val h = tableT[k + 1] - tableT[k]
        if (h <= 0.0) return tableS[k]
        val x = (t - tableT[k]) / h
        val x2 = x * x
        val x3 = x2 * x
        val s = (2 * x3 - 3 * x2 + 1) * tableS[k] + (x3 - 2 * x2 + x) * h * speed0[k] +
                (-2 * x3 + 3 * x2) * tableS[k + 1] + (x3 - x2) * h * speed1[k]
        return s.coerceIn(tableS[k], tableS[k + 1])
    }
    //</editor-fold>

    /**
     * the points at equal distances along the path with their parameters of [originalPath], about one per unit of
     * length times [lengthScale]; a closed path has the start again at its end, with the parameter 1.0. Computed on
     * first use
     */
    val points: List<Pair<T, Double>> by lazy {
        if (originalPath.empty) return@lazy emptyList()
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

/** the intervals that the table of lengths of a [RectifiedPath] starts with on a curved segment */
private const val INITIAL_INTERVALS = 4

/** the most times the intervals of the table of lengths of a [RectifiedPath] are halved */
private const val MAX_DEPTH = 12

/** the nodes and weights of 5-point Gauss-Legendre quadrature on [-1, 1] */
private val GAUSS_X = doubleArrayOf(
    -0.9061798459386640, -0.5384693101056831, 0.0, 0.5384693101056831, 0.9061798459386640
)
private val GAUSS_W = doubleArrayOf(
    0.2369268850561891, 0.4786286704993665, 0.5688888888888889, 0.4786286704993665, 0.2369268850561891
)

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
