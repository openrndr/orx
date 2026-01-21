package org.openrndr.extra.shapes.pose

import org.openrndr.extra.shapes.frames.computeClosedRMF
import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.math.*

import org.openrndr.math.Matrix33
import org.openrndr.math.Vector3
import kotlin.math.*



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
    val frames = computeClosedRMF(
        path.points.map { it.first },
        path.points.map { path.direction(it.second).normalized },
        null,

    )


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
        val rt = path.inverseRectify(t)
        val direction = path.direction(t).normalized
        val position = path.position(t)
        if (t < 0) {
            return Matrix44.fromColumnVectors(
                frames[0][0].xyz0,
                frames[0][1].xyz0,
                frames[0][2].xyz0,
                position.xyz1
            )
        } else
            if (t >= 1.0) {
                val li = frames.lastIndex
                return Matrix44.fromColumnVectors(
                    frames[li][0].xyz0,
                    frames[li][1].xyz0,
                    frames[li][2].xyz0,
                    position.xyz1
                )
            } else {
                val t0 = (rt * (frames.size - 1.0))
                val i0 = t0.toInt().coerceIn(frames.indices)
                val i1 = (i0 + 1).coerceIn(frames.indices)
                val f = t0 - i0

                val m0 = frames[i0]
                val m1 = frames[i1]

                val mi = rotationMinimizingFrame(m0, m1, f, direction)
                val pi = Matrix44.fromColumnVectors(mi[0].xyz0, mi[1].xyz0, mi[2].xyz0, position.xyz1)
                return pi
            }
    }

    /**
     * Computes the numerical derivative of the pose (transformation matrix) at a specified position
     * on the path, using central difference approximation.
     *
     * @param t The normalized position along the path, where 0.0 corresponds to the start
     *          and 1.0 corresponds to the end.
     * @param eps The small value used for central difference approximation, representing the step size.
     *            Defaults to 1E-6.
     * @return A `Matrix44` representing the derivative of the transformation matrix at the specified position.
     */
    fun poseDerivative(t: Double, eps: Double = 1E-2): Matrix44 {
        return (pose(t + eps) - pose(t - eps)) / (2.0 * eps)
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


/**
 * Rotation Minimizing Frame interpolation using swing-twist decomposition.
 * This method explicitly minimizes rotation around the constrained axis.
 *
 * @param m0 Initial orthonormal basis matrix
 * @param m1 Final orthonormal basis matrix
 * @param t Interpolation parameter [0, 1]
 * @param fT Constrained direction vector (should be normalized)
 * @return Interpolated orthonormal matrix with minimal torsion
 */
fun rotationMinimizingFrame(m0: Matrix33, m1: Matrix33, t: Double, fT: Vector3): Matrix33 {


    // Handle boundary cases
//    if (t <= 0.0) return m0
//    if (t >= 1.0) return m1

    // Extract basis vectors from M0
    val z0 = m0[2]
    val x0 = m0[0]
    val y0 = m0[1]


    // Extract basis vectors from M1
    val z1 = m1[2]
    val x1 = m1[0]


    val xt = m0[0].mix(m1[0], t).normalized
    val yt = m0[1].mix(m1[1], t).normalized
    val zt = m0[2].mix(m1[2], t).normalized


    return Matrix33.fromColumnVectors(xt, yt, fT.normalized)

}

/**
 * Compute the "swing" rotation that takes vector 'from' to 'to'
 * with no twist around the destination axis.
 */
private fun computeSwingRotation(from: Vector3, to: Vector3): Matrix33 {
    val from = from.normalized
    val to = to.normalized
    val axis = from cross to
    val axisLength = axis.length

    // If vectors are parallel or anti-parallel
    if (axisLength < 1e-10) {
        val dot = from dot to
        if (dot > 0) {
            // Same direction - no rotation needed
            return Matrix33.IDENTITY
        } else {
            // Opposite direction - 180 degree rotation around any perpendicular axis
            val perp = if (abs(from.x) < 0.9) {
                (Vector3.UNIT_X cross from).normalized
            } else {
                (Vector3.UNIT_Y cross from).normalized
            }
            return axisAngleToMatrix(perp, PI)
        }
    }

    val axisNorm = axis / axisLength
    val angle = acos(clamp((from dot to), -1.0, 1.0))

    return axisAngleToMatrix(axisNorm, angle)
}

/**
 * Project vector v onto the plane perpendicular to normal n
 */
private fun projectOntoPlane(v: Vector3, n: Vector3): Vector3 {
    val projected = v - n * (v dot n)
    val length = projected.length
    return if (length < 1e-10) {
        // If projection is near zero, find any perpendicular vector
        if (abs(n.x) < 0.9) {
            (Vector3.UNIT_X cross n).normalized
        } else {
            (Vector3.UNIT_Y cross n).normalized
        }
    } else {
        projected / length
    }
}

/**
 * Convert axis-angle representation to rotation matrix using Rodrigues' formula
 */
private fun axisAngleToMatrix(axis: Vector3, angle: Double): Matrix33 {
    val c = cos(angle)
    val s = sin(angle)
    val t = 1.0 - c

    val x = axis.x
    val y = axis.y
    val z = axis.z

    return Matrix33(
        t * x * x + c, t * x * y - s * z, t * x * z + s * y,
        t * x * y + s * z, t * y * y + c, t * y * z - s * x,
        t * x * z - s * y, t * y * z + s * x, t * z * z + c
    )
}

/**
 * Interpolate between two angles, taking the shortest path
 */
private fun interpolateAngle(a0: Double, a1: Double, t: Double): Double {
    var diff = a1 - a0

    // Normalize to [-π, π]
    while (diff > PI) diff -= 2 * PI
    while (diff < -PI) diff += 2 * PI

    return a0 + diff * t
}

/**
 * Clamp value between min and max
 */
private fun clamp(value: Double, min: Double, max: Double): Double {
    return max(min, min(max, value))
}


