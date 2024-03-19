package org.openrndr.extra.shapes.rectify

import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import kotlin.math.floor

class RectifiedPath3D(contour: Path3D, distanceTolerance: Double = 0.5, lengthScale: Double = 1.0) :
    RectifiedPath<Vector3>(contour, distanceTolerance, lengthScale) {

    override fun sub(t0: Double, t1: Double): Path3D {
        path as Path3D
        if (path.empty) {
            return Path3D(emptyList(), false)
        }

        return if (path.closed) {
            path.sub(rectify(t0.mod(1.0)) + floor(t0), rectify(t1.mod(1.0)) + floor(t1))
        } else {
            path.sub(rectify(t0), rectify(t1))
        }
    }

    override fun splitAt(ascendingTs: List<Double>, weldEpsilon: Double): List<Path3D> {
        @Suppress("UNCHECKED_CAST")
        return super.splitAt(ascendingTs, weldEpsilon) as List<Path3D>
    }
}
