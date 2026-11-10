package org.openrndr.extra.shapes.pose

import org.openrndr.extra.shapes.frames.applyTwist
import org.openrndr.extra.shapes.frames.computeClosureError
import org.openrndr.extra.shapes.frames.computeOpenRMF
import org.openrndr.extra.shapes.frames.initialFrameWithUp
import org.openrndr.extra.shapes.frames.parallelTransport
import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.math.*

import org.openrndr.math.Matrix33
import org.openrndr.math.Vector3
import kotlin.math.*



/**
 * Represents a 3D pose path defined along a rectified path: a frame at every point of the path whose z axis is the
 * direction of the path, which turns as little as possible from point to point.
 *
 * The first frame has its y axis along [up], as far as the direction of the path at its start allows. An open path
 * carries that frame along as it is; on a closed path the twist that the frame gathers in a lap is spread evenly
 * over the lap, so that the frames meet at the start.
 *
 * @property path The rectified 3D path for which the pose is calculated.
 * @param up The upward direction of the first frame; defaults to `Vector3.UNIT_Y`.
 * @param samples the number of frames that are computed at equal steps along the path, at least 256, and 64 for every
 * segment of the path; more frames are added where the path turns more than [maxTurn] between them
 * @param maxTurn the largest angle, in radians, by which the direction of the path turns between neighbouring frames,
 * as far as halving the steps between them up to 16 times allows; defaults to 1 degree
 */
class PosePath3D(
    val path: RectifiedPath3D,
    up: Vector3 = Vector3.UNIT_Y,
    samples: Int = maxOf(256, 64 * path.path.segments.size),
    maxTurn: Double = PI / 180.0
) {
    /** the rectified parameters of the frames, ascending from 0 to 1 */
    private val ts: DoubleArray

    /** the directions of the path at the frames */
    private val tangents: Array<Vector3>

    init {
        val count = samples.coerceAtLeast(2)
        val cosMaxTurn = cos(maxTurn)
        val tl = ArrayList<Double>(count)
        val dl = ArrayList<Vector3>(count)
        // the steps from t0 to t1, halved until the direction turns at most maxTurn in each
        fun add(t0: Double, d0: Vector3, t1: Double, d1: Vector3, depth: Int) {
            if (depth < MAX_DEPTH && (d0 dot d1) < cosMaxTurn) {
                val tm = (t0 + t1) / 2
                val dm = tangentAt(tm)
                add(t0, d0, tm, dm, depth + 1)
                add(tm, dm, t1, d1, depth + 1)
            } else {
                tl.add(t1); dl.add(d1)
            }
        }
        tl.add(0.0); dl.add(tangentAt(0.0))
        for (i in 1 until count) {
            val t1 = i.toDouble() / (count - 1)
            add(tl.last(), dl.last(), t1, tangentAt(t1), 0)
        }
        ts = tl.toDoubleArray()
        tangents = dl.toTypedArray()
    }

    /**
     * the direction of the path at the rectified [t]: that of its segment, or from nearby positions where the segment
     * has none, like at a cusp
     */
    private fun tangentAt(t: Double): Vector3 = tangentAtParameter(t, path.rectify(path.safe(t)))

    /** the direction of the path at the rectified [t] and the parameter [u] of the path there */
    private fun tangentAtParameter(t: Double, u: Double): Vector3 {
        val d = path.path.direction(u)
        if (d.squaredLength > 1E-20 && d.x.isFinite()) return d.normalized
        val h = 1E-5
        val ahead = path.position((t + h).let { if (path.path.closed) it else it.coerceAtMost(1.0) }) -
                path.position((t - h).let { if (path.path.closed) it else it.coerceAtLeast(0.0) })
        return ahead.normalized
    }

    /**
     * the frames at the rectified parameters at which they are computed: at equal steps, with more where the path
     * turns quickly
     */
    val frames: List<Matrix33> = run {
        val open = computeOpenRMF(tangents.asList(), initialFrameWithUp(tangents[0], up))
        if (path.path.closed && ts.size >= 3) {
            // the twist gathered in a lap, spread over the lap by the rectified parameter
            val closureError = computeClosureError(open[0], open.last(), tangents[0])
            open.mapIndexed { i, frame -> applyTwist(frame, tangents[i], -closureError * ts[i]) }
        } else {
            open
        }
    }

    /** the index of the frame at or before the rectified [t], from 0 to the second to last */
    private fun frameBefore(t: Double): Int {
        var lo = 0
        var hi = ts.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (ts[mid] <= t) lo = mid else hi = mid
        }
        return lo
    }

    /** the frame at the rectified [st], with [direction] its z axis, and the twist about it from frame to frame */
    private fun frameAt(st: Double, direction: Vector3): Pair<Matrix33, Double> {
        // the frames before and after t, both turned to the direction at t, between which it twists
        val i0 = frameBefore(st)
        val f = ((st - ts[i0]) / (ts[i0 + 1] - ts[i0])).coerceIn(0.0, 1.0)
        val a = parallelTransport(frames[i0], tangents[i0], direction)
        val b = parallelTransport(frames[i0 + 1], tangents[i0 + 1], direction)
        val twist = computeClosureError(a, b, direction)
        return applyTwist(a, direction, twist * f) to twist / (ts[i0 + 1] - ts[i0])
    }

    /**
     * Calculates the pose (transformation matrix) at a specified position on the path.
     * The pose includes translation and orientation, where orientation is interpolated
     * between frames based on the position parameter `t`.
     *
     * @param t A value between 0.0 and 1.0, representing the normalized position along the path, as for
     *          [RectifiedPath3D.position]. `t = 0.0` corresponds to the start of the path, and `t = 1.0` to the end;
     *          a closed path repeats.
     * @return A `Matrix44` representing the transformation at the specified position,
     *         including both translation and orientation.
     */
    fun pose(t: Double): Matrix44 {
        if (path.path.empty) return Matrix44.IDENTITY
        val st = path.safe(t)
        val u = path.rectify(st)
        val position = path.path.position(u)
        val direction = tangentAtParameter(st, u)
        val m = frameAt(st, direction).first
        return Matrix44.fromColumnVectors(m[0].normalized.xyz0, m[1].normalized.xyz0, direction.xyz0, position.xyz1)
    }

    /**
     * Computes the derivative of the pose (transformation matrix) by the rectified parameter at a specified position
     * on the path: the translation moves along the direction of the path at the speed of its length, and the frame
     * turns with the direction of the path and twists about it as the frames do.
     *
     * @param t The normalized position along the path, where 0.0 corresponds to the start
     *          and 1.0 corresponds to the end.
     * @param eps The step by which the change of the direction of the path is measured, in the rectified parameter;
     *            one-sided at the ends of an open path. Defaults to 1E-6.
     * @return A `Matrix44` representing the derivative of the transformation matrix at the specified position.
     */
    fun poseDerivative(t: Double, eps: Double = 1E-6): Matrix44 {
        if (path.path.empty) return Matrix44.ZERO
        val st = path.safe(t)
        val u = path.rectify(st)
        val direction = tangentAtParameter(st, u)
        val (frame, twistRate) = frameAt(st, direction)
        // the change of the direction by the rectified parameter
        val closed = path.path.closed
        val t0 = if (closed) st - eps else (st - eps).coerceAtLeast(0.0)
        val t1 = if (closed) st + eps else (st + eps).coerceAtMost(1.0)
        val dDirection = (tangentAt(t1) - tangentAt(t0)) / (t1 - t0)
        // the angular velocity of the frame: the turn of the direction, without a spin about it, and the twist
        val omega = (direction cross dDirection) + direction * twistRate
        val x = frame[0].normalized
        val y = frame[1].normalized
        val velocity = direction * path.path.length
        return Matrix44.fromColumnVectors(
            (omega cross x).xyz0, (omega cross y).xyz0, (omega cross direction).xyz0, velocity.xyz0
        )
    }
}

/** the most times the steps between the frames of a [PosePath3D] are halved */
private const val MAX_DEPTH = 16

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


