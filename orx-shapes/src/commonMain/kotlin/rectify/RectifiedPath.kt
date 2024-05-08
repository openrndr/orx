package org.openrndr.extra.shapes.rectify

import org.openrndr.extra.shapes.utilities.splitAt
import org.openrndr.extra.shapes.utilities.splitAtBase
import org.openrndr.math.EuclideanVector
import org.openrndr.math.clamp
import org.openrndr.shape.Path
import org.openrndr.shape.ShapeContour

/**
 * RectifiedContour provides an approximately uniform parameterization for [ShapeContour]
 */
abstract class RectifiedPath<T : EuclideanVector<T>>(
    open val path: Path<T>,
    distanceTolerance: Double = 0.5,
    lengthScale: Double = 1.0
) {
    val points =
        path.equidistantPositionsWithT((path.length * lengthScale).toInt().coerceAtLeast(2), distanceTolerance)

    val intervals by lazy {
        points.zipWithNext().map {
            Pair(it.first.second, it.second.second)
        }
    }

    internal fun safe(t: Double): Double {
        return if (path.closed) {
            t.mod(1.0)
        } else {
            t.clamp(0.0, 1.0)
        }
    }

    /**
     * computes a rectified t-value for [path]
     */
    fun rectify(t: Double): Double {
        if (path.empty) {
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

    fun inverseRectify(t: Double): Double {
        if (path.empty) {
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
        return if (path.empty) {
            path.infinity
        } else {
            path.position(rectify(safe(t)))
        }
    }

    fun direction(t: Double): T {
        return if (path.empty) {
            path.infinity
        } else {
            path.direction(rectify(safe(t)))
        }
    }

    abstract fun sub(t0: Double, t1: Double): Path<T>

    /**
     * Split contour at [ascendingTs]
     * @since orx 0.4.4
     */
    open fun splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<Path<T>> {
        return path.splitAtBase(ascendingTs.map { rectify(it) }, weldEpsilon)
    }
}