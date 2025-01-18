package org.openrndr.extra.shapes.rectify

import org.openrndr.extra.shapes.utilities.splitAtBase
import org.openrndr.math.EuclideanVector
import org.openrndr.math.clamp
import org.openrndr.shape.Path
import org.openrndr.shape.ShapeContour

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
    private val candidatePoints =
        originalPath.equidistantPositionsWithT((originalPath.length * lengthScale).toInt().coerceAtLeast(2), distanceTolerance)

    val points = if (originalPath.closed) candidatePoints + candidatePoints.first().copy(second = 1.0) else candidatePoints

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
     * computes a rectified t-value for [originalPath]
     */
    fun rectify(t: Double): Double {
        if (originalPath.empty) {
            return 0.0
        } else {
            if (t <= 0.0) {
                return 0.0
            }
            val fi = t * (points.size - 1.0)
            val fr = fi.mod(1.0)
            val i0 = fi.toInt()
            val i1 = i0 + 1

            return if (i0 >= points.size - 1) {
                1.0
            } else {
                (points[i0].second * (1.0 - fr) + points[i1].second * fr)
            }
        }
    }

    /**
     * Computes an inverse rectified t-value for the given normalized `t` parameter.
     * This method determines the original parameter space value from a rectified
     * (uniformly distributed) parameter space value.
     *
     * @param t A normalized parameter (between 0.0 and 1.0) in rectified parameter space.
     *          Values outside this range will be clamped to 0.0 or 1.0.
     * @return A normalized parameter (between 0.0 and 1.0) in the original parameter space.
     *         Returns 0.0 if the original path is empty.
     */
    fun inverseRectify(t: Double): Double {
        if (originalPath.empty) {
            return 0.0
        } else {
            if (t <= 0.0) {
                return 0.0
            } else if (t >= 1.0) {
                return 1.0
            } else {
                val index = intervals.binarySearch {
                    if (t < it.first) {
                        1
                    } else if (t > it.second) {
                        -1
                    } else {
                        0
                    }
                }
                val t0 = t - intervals[index].first
                val dt = intervals[index].second - intervals[index].first
                val f = t0 / dt
                val f0 = index.toDouble() / intervals.size
                val f1 = (index + 1.0) / intervals.size

                return f0 * (1.0 - f) + f1 * f
            }
        }
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