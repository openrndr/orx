package org.openrndr.extra.shapes.rectify

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import kotlin.math.floor

/**
 * RectifiedContour provides an approximately uniform parameterization for [ShapeContour]
 */
class RectifiedContour(val contour: ShapeContour, distanceTolerance: Double = 0.5, lengthScale: Double = 1.0, ) {
    val points = contour.equidistantPositionsWithT((contour.length * lengthScale).toInt().coerceAtLeast(2), distanceTolerance)

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

    fun position(t: Double): Vector2 {
        return contour.position(rectify(safe(t)))
    }

    fun velocity(t: Double): Vector2 {
        val (segment, st) = contour.segment(rectify(safe(t)))
        return contour.segments[segment].direction(st)
    }

    fun normal(t: Double): Vector2 {
        return contour.normal(rectify(safe(t)))
    }

    fun pose(t: Double): Matrix44 {
        return contour.pose(rectify(safe(t)))
    }

    fun sub(t0: Double, t1: Double): ShapeContour {
        return if (contour.closed) {
            contour.sub(rectify(t0.mod(1.0)) + floor(t0), rectify(t1.mod(1.0)) + floor(t1))
        } else {
            contour.sub(rectify(t0), rectify(t1))
        }
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