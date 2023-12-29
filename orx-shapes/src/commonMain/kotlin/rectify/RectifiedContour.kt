package org.openrndr.extra.shapes.rectify

import org.openrndr.extra.shapes.utilities.splitAt
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import kotlin.math.floor

/**
 * RectifiedContour provides an approximately uniform parameterization for [ShapeContour]
 */
class RectifiedContour(val contour: ShapeContour, distanceTolerance: Double = 0.5, lengthScale: Double = 1.0) {
    val points =
        contour.equidistantPositionsWithT((contour.length * lengthScale).toInt().coerceAtLeast(2), distanceTolerance)

    val intervals by lazy {
        points.zipWithNext().map {
            Pair(it.first.second, it.second.second)
        }
    }

    private fun safe(t: Double): Double {
        return if (contour.closed) {
            t.mod(1.0)
        } else {
            t.clamp(0.0, 1.0)
        }
    }

    /**
     * computes a rectified t-value for [contour]
     */
    fun rectify(t: Double): Double {
        if (contour.empty) {
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
        if (contour.empty) {
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

    fun position(t: Double): Vector2 {
        return if (contour.empty) {
            Vector2.INFINITY
        } else {
            contour.position(rectify(safe(t)))
        }
    }

    fun velocity(t: Double): Vector2 {
        return if (contour.empty) {
            Vector2.ZERO
        } else {
            val (segment, st) = contour.segment(rectify(safe(t)))
            contour.segments[segment].direction(st)
        }
    }

    fun normal(t: Double): Vector2 {
        return if (contour.empty) {
            Vector2.UNIT_Y
        } else {
            contour.normal(rectify(safe(t)))
        }
    }

    fun pose(t: Double): Matrix44 {
        return if (contour.empty) {
            Matrix44.IDENTITY
        } else {
            contour.pose(rectify(safe(t)))
        }
    }

    fun sub(t0: Double, t1: Double): ShapeContour {
        if (contour.empty) {
            return ShapeContour.EMPTY
        }
        return if (contour.closed) {
            contour.sub(rectify(t0.mod(1.0)) + floor(t0), rectify(t1.mod(1.0)) + floor(t1))
        } else {
            contour.sub(rectify(t0), rectify(t1))
        }
    }

    /**
     * Split contour at [ascendingTs]
     * @since orx 0.4.4
     */
    fun splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<ShapeContour> {
        return contour.splitAt(ascendingTs.map { rectify(it) }, weldEpsilon)
    }
}

/** create a rectified contour
 * @param distanceTolerance distance tolerance to use, 0.5 is the default distance tolerance
 * @param lengthScale used to compute the size of the LUT, default value is 1.0
 **/
fun ShapeContour.rectified(distanceTolerance: Double = 0.5, lengthScale: Double = 1.0): RectifiedContour {
    return RectifiedContour(this, distanceTolerance, lengthScale)
}

/**  create a rectified contour
 *   @param distanceTolerance distance tolerance to use, 0.5 is the default distance tolerance
 *   @param lengthScale used to compute the size of the LUT, default value is 1.0
 *
 * */
fun Segment.rectified(distanceTolerance: Double = 0.5, lengthScale: Double = 1.0): RectifiedContour {
    return RectifiedContour(this.contour, distanceTolerance, lengthScale)
}