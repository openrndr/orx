package org.openrndr.extra.shapes.frames

import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Calculates rotation minimizing frames (pose matrices) at these positions, see [frames].
 * @param up0 the up of the first frame, as far as the direction of the path at its start allows
 * @param closed true when the positions go round a closed path, see [frames]
 */
fun List<Vector3>.frames(up0: Vector3, closed: Boolean = false): List<Matrix44> =
    frames(this, up0 = up0, closed = closed)

/**
 * Calculates rotation minimizing frames along a series of positions: one frame for every position, at that position,
 * whose z axis is the direction of the path there and which turns as little as possible about it from frame to frame.
 * The frames are carried from position to position by double reflection (Wang et al. 2008), which follows the frames
 * of the curve through the positions to the fourth order of the steps between them.
 *
 * The first frame has its y axis along [up0], as far as the direction of the path at its start allows, and its x axis
 * to the right: y cross z.
 *
 * @param positions a list of 3D positions defining the path.
 * @param directions the directions of the path at the positions; where one is missing, empty or not finite, the
 * direction is that of a parabola through the position and its neighbours.
 * @param up0 the initial up vector, must not have zero or NaN length.
 * @param closed true when the positions go round a closed path: the path goes on from the last position to the first,
 * or the last position is the first again; the twist that the frames gather in a lap is then spread over the lap, by
 * the lengths between the positions, so that the frames meet at the start.
 * @return a list of 4x4 frame matrices corresponding to the input positions.
 */
fun frames(
    positions: List<Vector3>,
    directions: List<Vector3> = emptyList(),
    up0: Vector3,
    closed: Boolean = false
): List<Matrix44> {
    require(up0.squaredLength > 0.0) {
        "up0 ($up0) has 0 or NaN length"
    }
    if (positions.isEmpty()) return emptyList()
    val n = positions.size
    // a closed path whose last position is its first again
    val repeats = closed && n > 2 && positions.last().squaredDistanceTo(positions.first()) < 1E-24

    val tangents = List(n) { i ->
        directions.getOrNull(i)?.takeIf { it.squaredLength > 1E-24 && it.x.isFinite() && it.y.isFinite() && it.z.isFinite() }
            ?.normalized ?: estimateTangent(positions, i, closed, repeats)
    }

    // the up of every frame, carried from position to position
    val ups = ArrayList<Vector3>(n)
    ups.add(initialFrameWithUp(tangents[0], up0)[1])
    for (i in 1 until n) ups.add(reflect(ups[i - 1], positions[i - 1], tangents[i - 1], positions[i], tangents[i], up0))

    if (closed && n > 2) {
        // the up carried on round to the start, against the first up: the twist of a lap
        val end = if (repeats) ups.last()
        else reflect(ups.last(), positions.last(), tangents.last(), positions[0], tangents[0], up0)
        val z = tangents[0]
        val a = ups[0]
        val twist = atan2((end cross a) dot z, (end dot a).coerceIn(-1.0, 1.0))
        // spread by the lengths from the start
        val lengths = DoubleArray(n)
        for (i in 1 until n) lengths[i] = lengths[i - 1] + positions[i].distanceTo(positions[i - 1])
        val lap = if (repeats) lengths.last() else lengths.last() + positions.last().distanceTo(positions[0])
        if (lap > 0.0) {
            for (i in 0 until n) {
                val angle = twist * lengths[i] / lap
                val t = tangents[i]
                ups[i] = ups[i] * cos(angle) + (t cross ups[i]) * sin(angle)
            }
        }
    }

    return List(n) { i ->
        val z = tangents[i]
        val y = ups[i]
        val x = (y cross z).normalized
        Matrix44.fromColumnVectors(x.xyz0, y.xyz0, z.xyz0, positions[i].xyz1)
    }
}

/**
 * The up [up] of a frame at [p0] with direction [t0] carried to [p1] with direction [t1] by double reflection: across
 * the plane between the positions, then across the plane between the reflected direction and [t1]
 */
private fun reflect(up: Vector3, p0: Vector3, t0: Vector3, p1: Vector3, t1: Vector3, up0: Vector3): Vector3 {
    var r = up
    var t = t0
    val v1 = p1 - p0
    val c1 = v1 dot v1
    if (c1 > 1E-24) {
        r -= v1 * (2.0 / c1 * (v1 dot r))
        t -= v1 * (2.0 / c1 * (v1 dot t))
    }
    val v2 = t1 - t
    val c2 = v2 dot v2
    if (c2 > 1E-24) {
        r -= v2 * (2.0 / c2 * (v2 dot r))
    }
    // across the new direction, of unit length
    val across = r - t1 * (r dot t1)
    return if (across.squaredLength > 1E-24) across.normalized else initialFrameWithUp(t1, up0)[1]
}

/**
 * The direction at position [i] of a parabola through it and its neighbours, by the distances between them; at the
 * ends of an open path through the end and the two positions next to it. Positions at the same place are passed over.
 */
private fun estimateTangent(positions: List<Vector3>, i: Int, closed: Boolean, repeats: Boolean): Vector3 {
    val n = positions.size
    if (n < 2) return Vector3.UNIT_Z
    // the positions of a lap, without the repeated start
    val m = if (repeats) n - 1 else n
    val start = if (repeats && i == n - 1) 0 else i
    val p = positions[start]

    // the index of the nearest position from [from] in the direction of [step] that is not at [at]
    fun next(from: Int, step: Int, at: Vector3): Int? {
        var j = from
        repeat(m) {
            j += step
            if (closed) j = ((j % m) + m) % m else if (j !in 0 until n) return null
            if (positions[j].squaredDistanceTo(at) > 1E-24) return j
        }
        return null
    }

    val b = next(start, -1, p)
    val a = next(start, 1, p)
    // the derivative at the first of three positions with distances h0 and h1 between them
    fun endDerivative(p0: Vector3, p1: Vector3, p2: Vector3): Vector3 {
        val h0 = p1.distanceTo(p0)
        val h1 = p2.distanceTo(p1)
        return (p1 - p0) * ((2 * h0 + h1) / (h0 * (h0 + h1))) - (p2 - p1) * (h0 / (h1 * (h0 + h1)))
    }
    return when {
        b != null && a != null -> {
            val before = positions[b]
            val after = positions[a]
            val h0 = p.distanceTo(before)
            val h1 = after.distanceTo(p)
            ((p - before) * (h1 / (h0 * (h0 + h1))) + (after - p) * (h0 / (h1 * (h0 + h1)))).normalized
        }
        a != null -> {
            // the start of an open path, by the next two positions
            val further = next(a, 1, positions[a])
            if (further == null) (positions[a] - p).normalized
            else endDerivative(p, positions[a], positions[further]).normalized
        }
        b != null -> {
            // the end of an open path, by the two positions before it, as the start of the path backwards
            val further = next(b, -1, positions[b])
            if (further == null) (p - positions[b]).normalized
            else -endDerivative(p, positions[b], positions[further]).normalized
        }
        else -> Vector3.UNIT_Z
    }
}

/**
 * Generates rotation minimizing frames for an open curve: the first frame carried along the curve by parallel
 * transport, without a twist to make the ends of a closed curve meet.
 *
 * @param tangents the directions of the curve at its sampled points
 * @param initialFrame the frame at the first point, generated from the first tangent when null
 */
internal fun computeOpenRMF(tangents: List<Vector3>, initialFrame: Matrix33? = null): List<Matrix33> {
    require(tangents.isNotEmpty()) { "Need at least 1 tangent" }
    val frames = mutableListOf(initialFrame ?: generateInitialFrame(tangents[0]))
    for (i in 1 until tangents.size) frames.add(parallelTransport(frames[i - 1], tangents[i - 1], tangents[i]))
    return frames
}

/**
 * The frame at a point with the direction [tangent] whose y axis is [up] in the plane across the tangent, x pointing
 * right; a frame from [generateInitialFrame] when [up] is along the tangent
 */
internal fun initialFrameWithUp(tangent: Vector3, up: Vector3): Matrix33 {
    val z = tangent.normalized
    val across = up - z * (up dot z)
    if (across.length < 1E-9) return generateInitialFrame(z)
    val y = across.normalized
    val x = y cross z
    return Matrix33.fromColumnVectors(x, y, z)
}

/**
 * Generate an initial orthonormal frame given a tangent vector
 */
internal fun generateInitialFrame(tangent: Vector3): Matrix33 {
    // Choose a vector not parallel to tangent
    val arbitrary = if (abs(tangent.x) < 0.9) {
        Vector3.UNIT_X
    } else {
        Vector3.UNIT_Y
    }

    // Use Gram-Schmidt to create orthonormal basis
    val binormal = (tangent cross arbitrary).normalized
    val normal = binormal cross tangent

    return Matrix33.fromColumnVectors(normal, binormal, tangent)
}

/**
 * Parallel transport a frame from one tangent to another (rotation minimizing)
 * This is the Double Reflection Method (Hanson & Ma)
 */
internal fun parallelTransport(frame: Matrix33, t0: Vector3, t1: Vector3): Matrix33 {
    val x0 = frame[0] //Vector3(frame.c0r0, frame.c0r1, frame.c0r2)
    val y0 = frame[1] //Vector3(frame.c1r0, frame.c1r1, frame.c1r2)

    // Compute the rotation that takes t0 to t1
    val b = t0 + t1
    val bLength = b.length

    val x1: Vector3
    val y1: Vector3

    if (bLength < 1e-10) {
        // t0 and t1 are opposite - 180 degree rotation
        x1 = -x0
        y1 = -y0
    } else {
        val bNorm = b / bLength

        // Double reflection formula
        x1 = x0 - bNorm * (2.0 * (x0 dot bNorm))
        y1 = y0 - bNorm * (2.0 * (y0 dot bNorm))
    }

    return Matrix33.fromColumnVectors(x1, y1, t1)
}

/**
 * Compute the twist angle between two frames with the same tangent
 */
internal fun computeClosureError(frame0: Matrix33, frameN: Matrix33, tangent: Vector3): Double {
    val x0 = frame0[0] //Vector3(frame0.c0r0, frame0.c0r1, frame0.c0r2)
    val xN = frameN[0] //Vector3(frameN.c0r0, frameN.c0r1, frameN.c0r2)

    // Project both x-vectors onto plane perpendicular to tangent
    val x0Proj = (x0 - tangent * (x0 dot tangent)).normalized
    val xNProj = (xN - tangent * (xN dot tangent)).normalized

    // Compute angle between them
    val cosAngle = clamp(x0Proj dot xNProj, -1.0, 1.0)
    val sinAngle = (x0Proj cross xNProj) dot tangent

    return atan2(sinAngle, cosAngle)
}

/**
 * Apply a twist rotation around the tangent to a frame
 */
internal fun applyTwist(frame: Matrix33, tangent: Vector3, angle: Double): Matrix33 {
    if (abs(angle) < 1e-10) return frame

    val x = frame[0] //Vector3(frame.c0r0, frame.c0r1, frame.c0r2)
    val y = frame[1] //Vector3(frame.c1r0, frame.c1r1, frame.c1r2)

    val c = cos(angle)
    val s = sin(angle)

    // Rotate x and y around tangent
    val xNew = x * c + y * s
    val yNew = -x * s + y * c

    return Matrix33.fromColumnVectors(xNew, yNew, tangent)
}

/**
 * Helper: clamp value
 */
private fun clamp(value: Double, min: Double, max: Double): Double {
    return max(min, min(max, value))
}
