package org.openrndr.extra.shapes.frames

import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.buildTransform
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Calculate frames (pose matrices) using parallel transport
 * @param up0 initial up vector, should not be collinear with `this[1] - this[0]`
 */

fun List<Vector3>.frames(up0: Vector3): List<Matrix44> = frames(this, up0 = up0)

/**
 * Calculates a list of frame transformation matrices using parallel transport along a series of positions.
 *
 * @param positions a list of 3D positions defining the path.
 * @param directions an optional list of direction vectors at each position for guiding forward orientation;
 *                   if empty, directions are estimated from the positions.
 * @param up0 the initial up vector, must not have zero or NaN length.
 * @return a list of 4x4 frame matrices corresponding to the input positions.
 */
fun frames(positions: List<Vector3>, directions: List<Vector3> = emptyList(), up0: Vector3): List<Matrix44> {

    require(up0.squaredLength > 0.0) {
        "up0 ($up0) has 0 or NaN length"
    }

    val result = mutableListOf<Matrix44>()

    if (positions.isEmpty()) {
        return emptyList()
    }

    if (positions.size == 1) {
        return listOf(Matrix44.IDENTITY)
    }

    var up = up0.normalized
    run {
        val current = positions[0]
        val next = positions[1]
        val forward = (directions.getOrNull(0) ?: (next - current)).normalized
        var right = (up cross forward).normalized
        up = ((forward cross right)).normalized
        right = (up cross forward).normalized
        val frame = Matrix44.fromColumnVectors(right.xyz0, up.xyz0, forward.xyz0, current.xyz1)
        require(frame.determinant in 0.99..1.01) {
            "Initial frame determinant (${frame.determinant}) != 1.0"
        }

        result.add(frame)
    }

    for (i in 1 until positions.size - 1) {
        val prev = positions[i - 1]
        val current = positions[i]
        val next = positions[i + 1]
        val f1 = (next - current).normalized
        val f0 = (current - prev).normalized

        val forward = (directions.getOrNull(i) ?: (f0 + f1)).normalized
        require(forward.length > 0.0) { "`forward.length` is zero or NaN in .frames()" }
        var right = (up cross forward).normalized
        up = ((forward cross right)).normalized
        right = (up cross forward).normalized

        require(up.length > 0.0) { "`up.length` is zero or NaN in .frames()" }
        require(right.length > 0.0) { "`right.length` is zero or NaN in .frames()" }

        val orientation = Matrix44.fromColumnVectors(right.xyz0, up.xyz0, forward.xyz0, Vector4.UNIT_W)
        require(orientation.determinant in 0.99..1.01) {
            "Orientation determinant ${orientation.determinant} != 1.0"
        }

        val m = buildTransform {
            translate(current)
            multiply(orientation)
        }

        result.add(m)
    }
    return result
}

/**
 * Generates rotation minimizing frames for a closed curve.
 *
 * @param points Sampled points on the closed curve where points[0] ≈ points[N-1]
 * @param tangents
 * @param initialFrame Optional initial frame at points[0]. If null, generates one.
 * @return List of orthonormal frames (Matrix33) for each point
 */
internal fun computeClosedRMF(points: List<Vector3>, tangents: List<Vector3>, initialFrame: Matrix33? = null): List<Matrix33> {
    val n = points.size
    require(n >= 3) { "Need at least 3 points" }

    // Step 2: Generate initial frame at point 0 if not provided
    val frame0 = initialFrame ?: generateInitialFrame(tangents[0])

    // Step 3: Propagate frames around the loop (without closure constraint)
    val frames = mutableListOf(frame0)
    for (i in 1 until n) {
        val prevFrame = frames[i - 1]
        val prevTangent = tangents[i - 1]
        val currTangent = tangents[i]

        val newFrame = parallelTransport(prevFrame, prevTangent, currTangent)
        frames.add(newFrame)
    }

    // Step 4: Measure the closure error (twist accumulated around the loop)
    val finalFrame = frames[n - 1]
    val closureError = computeClosureError(frame0, finalFrame, tangents[0])

    // Step 5: Distribute the closure error evenly around the curve
    val correctedFrames = mutableListOf<Matrix33>()
    for (i in 0 until n) {
        val t = i.toDouble() / (n - 1).toDouble()
        val twist = -closureError * t  // Negative to cancel out the error
        val twistedFrame = applyTwist(frames[i], tangents[i], twist)
        correctedFrames.add(twistedFrame)
    }

    return correctedFrames
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
