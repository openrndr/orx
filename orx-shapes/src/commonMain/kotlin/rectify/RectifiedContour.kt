package org.openrndr.extra.shapes.rectify

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.floor

class RectifiedContour(contour: ShapeContour, distanceTolerance: Double = 0.5, lengthScale: Double = 1.0) :
    RectifiedPath<Vector2>(contour, distanceTolerance, lengthScale) {
    fun velocity(t: Double): Vector2 {
        return if (path.empty) {
            Vector2.ZERO
        } else {
            val (segment, st) = path.segment(rectify(safe(t)))
            path.segments[segment].direction(st)
        }
    }

    fun normal(t: Double): Vector2 {
        return if (path.empty) {
            Vector2.UNIT_Y
        } else {
            (path as ShapeContour).normal(rectify(safe(t)))
        }
    }

    fun pose(t: Double): Matrix44 {
        path as ShapeContour
        return if (path.empty) {
            Matrix44.IDENTITY
        } else {
            path.pose(rectify(safe(t)))
        }
    }

    override fun sub(t0: Double, t1: Double): ShapeContour {
        path as ShapeContour
        if (path.empty) {
            return ShapeContour.EMPTY
        }

        return if (path.closed) {
            path.sub(rectify(t0.mod(1.0)) + floor(t0), rectify(t1.mod(1.0)) + floor(t1))
        } else {
            path.sub(rectify(t0), rectify(t1))
        }
    }

    override fun splitAt(ascendingTs: List<Double>, weldEpsilon: Double): List<ShapeContour> {
        @Suppress("UNCHECKED_CAST")
        return super.splitAt(ascendingTs, weldEpsilon) as List<ShapeContour>
    }
}
