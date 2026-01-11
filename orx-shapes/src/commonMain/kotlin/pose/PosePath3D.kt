package org.openrndr.extra.shapes.pose

import org.openrndr.extra.shapes.frames.frames
import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.math.*
import org.openrndr.math.transforms.translate

/**
 * Calculates a transformation matrix (frame) based on the specified forward and up vectors.
 *
 * This method adjusts the given up vector to ensure it is orthogonal to the forward vector,
 * then calculates a right vector using the cross product of the forward and up vectors.
 * Finally, the method constructs a transformation matrix using the computed forward, up,
 * and right vectors.
 *
 * @param forward The vector representing the forward direction.
 * @param up The vector representing the up direction (will be adjusted to maintain orthogonality with forward).
 * @return A transformation matrix (Matrix44) based on the given forward and adjusted up vectors.
 */
private fun findFrame(forward: Vector3, up: Vector3): Matrix44 {
    var up = up.normalized
    val right = (forward cross up).normalized
    up = ((right cross forward)).normalized

    val orientation = Matrix44.fromColumnVectors(right.xyz0, up.xyz0, -forward.xyz0, Vector4.UNIT_W)
    return orientation
}

/**
 * Represents a 3D pose path defined along a rectified path. The class provides interpolation
 * and orientation adjustments along the defined path.
 *
 * @property path The rectified 3D path for which the pose is calculated.
 * @constructor Initializes a `PosePath3D` instance with an optional upward direction vector.
 * @param path The rectified 3D path along which the poses will be calculated.
 * @param up The default upward direction vector, which is used to establish orientation; defaults to `Vector3.UNIT_Y`.
 */
class PosePath3D(val path: RectifiedPath3D, up: Vector3 = Vector3.UNIT_Y) {
    val frames = path.frames(path.points.map { it.second }, up, false).map { Quaternion.fromMatrix(it.matrix33) }

    /**
     * Calculates the pose (transformation matrix) at a specified position on the path.
     * The pose includes translation and orientation, where orientation is interpolated
     * between frames based on the position parameter `t`.
     *
     * @param t A value between 0.0 and 1.0, representing the normalized position along the path.
     *          `t = 0.0` corresponds to the start of the path, and `t = 1.0` corresponds to the end.
     * @return A `Matrix44` representing the transformation at the specified position,
     *         including both translation and orientation.
     */
    fun pose(t: Double): Matrix44 {
        val tr = Matrix44.translate(path.position(t))
        val direction = path.direction(t).normalized

        return tr * if (t <= 0.0) {
            findFrame(direction, frames[0].times(Vector3.UNIT_Y))
        } else if (t >= 1.0) {
            findFrame(direction, frames.last().times(Vector3.UNIT_Y))
        } else {
            val t0 = (t * frames.size)
            val i0 = t0.toInt().coerceIn(frames.indices)
            val i1 = (i0 + 1).coerceIn(frames.indices)

            val f = t0 - i0
            val o0 = frames[i0]
            val o1 = frames[i1]
            val o = slerp(o0, o1, f)
            findFrame(direction, o.times(Vector3.UNIT_Y))
        }
    }
}

/**
 * Generates a 3D pose path along the current rectified path. The resulting `PosePath3D`
 * provides interpolation and orientation adjustments along the path.
 *
 * @param up The upward direction vector used to establish orientation; defaults to `Vector3.UNIT_Y`.
 * @return A `PosePath3D` object representing the 3D pose path derived from the current `RectifiedPath3D`.
 */
fun RectifiedPath3D.pose(up: Vector3 = Vector3.UNIT_Y) = PosePath3D(this, up)