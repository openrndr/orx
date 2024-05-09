package org.openrndr.extra.shapes.rectify

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.floor

class RectifiedContour(contour: ShapeContour, distanceTolerance: Double = 0.5, lengthScale: Double = 1.0) :
    RectifiedPath<Vector2>(contour, distanceTolerance, lengthScale) {
    fun velocity(t: Double): Vector2 {
        return if (originalPath.empty) {
            Vector2.ZERO
        } else {
            val (segment, st) = originalPath.segment(rectify(safe(t)))
            originalPath.segments[segment].direction(st)
        }
    }

    fun normal(t: Double): Vector2 {
        return if (originalPath.empty) {
            Vector2.UNIT_Y
        } else {
            (originalPath as ShapeContour).normal(rectify(safe(t)))
        }
    }

    fun pose(t: Double): Matrix44 {
        originalPath as ShapeContour
        return if (originalPath.empty) {
            Matrix44.IDENTITY
        } else {
            originalPath.pose(rectify(safe(t)))
        }
    }

    override fun sub(t0: Double, t1: Double): ShapeContour {
        originalPath as ShapeContour
        if (originalPath.empty) {
            return ShapeContour.EMPTY
        }

        return if (originalPath.closed) {
            originalPath.sub(rectify(t0.mod(1.0)) + floor(t0), rectify(t1.mod(1.0)) + floor(t1))
        } else {
            originalPath.sub(rectify(t0), rectify(t1))
        }
    }

    override fun splitAt(ascendingTs: List<Double>, weldEpsilon: Double): List<ShapeContour> {
        @Suppress("UNCHECKED_CAST")
        return super.splitAt(ascendingTs, weldEpsilon) as List<ShapeContour>
    }

    val contour: ShapeContour get() = originalPath as ShapeContour
}
