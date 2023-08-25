package org.openrndr.extra.meshgenerators

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Writes quads to [writer] creating a surface that connects
 * [linearContour0] with [linearContour1].
 * The positions and orientations of the two contours
 * are defined by the [frame0] and [frame1] matrices.
 *
 * @param linearContour0 the first cross-section
 * @param linearContour1 the second cross-section
 * @param frame0 a transformation matrix with the pose of [linearContour0]
 * @param frame1 a transformation matrix with the pose of [linearContour1]
 * @param writer the vertex writer function
 */
fun contourSegment(
    linearContour0: List<Vector2>,
    linearContour1: List<Vector2>,
    frame0: Matrix44,
    frame1: Matrix44,
    writer: VertexWriter
) {
    for (i in linearContour0.indices) {
        val v0a = linearContour0[i]
        val v1a = linearContour0[(i + 1).mod(linearContour0.size)]

        val v0b = linearContour1[i]
        val v1b = linearContour1[(i + 1).mod(linearContour1.size)]

        val v00a = (frame0 * v0a.xy01).xyz
        val v01a = (frame0 * v1a.xy01).xyz

        val v10b = (frame1 * v0b.xy01).xyz
        val v11b = (frame1 * v1b.xy01).xyz
        val faceNormal = ((v10b - v00a).normalized cross (v01a - v00a).normalized).normalized
        quadToTris(v00a, v01a, v10b, v11b, faceNormal, writer)
    }
}

/**
 * Contour segment
 *
 * @param linearContour0
 * @param linearContour1
 * @param frame0
 * @param frame1
 * @param v0
 * @param v1
 * @param writer
 */
fun contourSegment(
    linearContour0: List<Vector2>,
    linearContour1: List<Vector2>,
    frame0: Matrix44,
    frame1: Matrix44,
    v0: Double,
    v1: Double,
    writer: VertexWriter
) {
    var i0 = 0
    var i1 = 0
    do {
        val flipNormal = false

        val vCurr0 = (frame0 * linearContour0[i0 % linearContour0.size].xy01).xyz
        val vCurr1 = (frame1 * linearContour1[i1 % linearContour1.size].xy01).xyz
        val vNext0 = (frame0 * linearContour0[(i0 + 1) % linearContour0.size].xy01).xyz
        val vNext1 = (frame1 * linearContour1[(i1 + 1) % linearContour1.size].xy01).xyz

        val tCurr0 = i0.toDouble() / linearContour0.size
        val tCurr1 = i1.toDouble() / linearContour1.size
        val tNext0 = (i0 + 1.0) / linearContour0.size
        val tNext1 = (i1 + 1.0) / linearContour1.size

        val uvCurr0 = Vector2(tCurr0, v0)
        val uvCurr1 = Vector2(tCurr1, v1)

        val vNext: Vector3
        val uvNext: Vector2

        if (tNext0 < tNext1 || (tNext0 == tNext1 && tCurr0 < tCurr1)) {
            ++i0
            vNext = vNext0
            uvNext = Vector2(i0.toDouble() / linearContour0.size, v0)
        } else {
            ++i1
            vNext = vNext1
            uvNext = Vector2(i1.toDouble() / linearContour1.size, v1)
        }

        val faceNormal = if (flipNormal)
            (vNext - vCurr1).cross(vCurr0 - vCurr1)
        else
            (vNext - vCurr0).cross(vCurr1 - vCurr0)

        writeTri(
            vCurr0, vNext, vCurr1,
            uvCurr0, uvNext, uvCurr1,
            faceNormal.normalized,
            writer
        )

    } while (i0 < linearContour0.size || i1 < linearContour1.size)
}

