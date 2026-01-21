package org.openrndr.extra.meshgenerators

import org.openrndr.extra.shapes.frames.frames
import org.openrndr.extra.shapes.pose.pose
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.asRadians
import org.openrndr.shape.Path3D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.cos
import kotlin.math.sin

/**
 * Writes quads to [writer] creating a surface that connects two
 * displaced instances of [linearContour]. The positions and orientations
 * of the two contours are defined by the [frame0] and [frame1] matrices.
 *
 * @param linearContour the cross-section of the surface to create
 * @param frame0 a transformation matrix that defines an initial position
 * @param frame1 a transformation matrix that defines a final position
 * @param writer the vertex writer function
 */
fun contourSegmentWithNormals(
    linearContour: List<Vector2>,
    linearContourNormals: List<Vector2>,
    frame0: Matrix44,
    frame1: Matrix44,
    writer: VertexWriter,
    useFaceNormals: Boolean
) {
    require(linearContour.size == linearContourNormals.size) { "linearContour and linearContourNormals must have the same size" }
    for (i in linearContour.indices) {
        val v0 = linearContour[i]
        val v1 = linearContour[(i + 1).mod(linearContour.size)]

        val n0 = linearContourNormals[i]
        val n1 = linearContourNormals[(i + 1).mod(linearContourNormals.size)]

        val nframe0 = frame0 //.inversed
        val nframe1 = frame1//.inversed

        val v00 = (frame0 * v0.xy01).xyz
        val v01 = (frame0 * v1.xy01).xyz

        val v10 = (frame1 * v0.xy01).xyz
        val v11 = (frame1 * v1.xy01).xyz

        val faceNormal = ((v10 - v00).normalized cross (v01 - v00).normalized).normalized
        val n00 = if (useFaceNormals) faceNormal else (nframe0 * n0.xy01).xyz
        val n01 = if (useFaceNormals) faceNormal else (nframe0 * n1.xy01).xyz

        val n10 = if (useFaceNormals) faceNormal else (nframe1 * n0.xy01).xyz
        val n11 = if (useFaceNormals) faceNormal else (nframe1 * n1.xy01).xyz

        quadToTris(v00, v01, v10, v11, n00, n01, n10, n11, writer)
    }
}

fun contourSegmentWithNormals(
    linearContour: List<Vector2>,
    linearContourNormals: List<Vector2>,
    scale0: Double, scale1: Double, dscale0: Double, dscale1: Double,
    torsion0: Double, torsion1: Double, dtorsion0: Double, dtorsion1: Double,
    frame0: Matrix44,
    frame1: Matrix44,
    dframe0: Matrix44,
    dframe1: Matrix44,
    tangent0: Vector3,
    tangent1: Vector3,
    writer: VertexWriter,
    useFaceNormals: Boolean = true
) {
    require(linearContour.size == linearContourNormals.size) { "linearContour and linearContourNormals must have the same size" }

    for (i in linearContour.indices) {
        val ps0 = linearContour[i]
        val ps1 = linearContour[(i + 1).mod(linearContour.size)]

        val dps0 = linearContourNormals[i]
        val dps1 = linearContourNormals[(i + 1).mod(linearContourNormals.size)]

        val theta0 = torsion0.asRadians
        val theta1 = torsion1.asRadians
        val dtheta0 = dtorsion0.asRadians
        val dtheta1 = dtorsion1.asRadians

        val ct0 = cos(theta0)
        val st0 = sin(theta0)
        val ct1 = cos(theta1)
        val st1 = sin(theta1)


        val vt0s0 = (frame0 * (ps0.rotate(torsion0) * scale0).xy01).xyz
        val vt0s1 = (frame0 * (ps1.rotate(torsion0) * scale0).xy01).xyz

        val vt1s0 = (frame1 * (ps0.rotate(torsion1) * scale1).xy01).xyz
        val vt1s1 = (frame1 * (ps1.rotate(torsion1) * scale1).xy01).xyz


        val Nt0 = frame0[0].xyz.normalized// * tangent0.length
        val Nt1 = frame1[0].xyz.normalized// * tangent1.length
        val Bt0 = frame0[1].xyz.normalized// * tangent0.length
        val Bt1 = frame1[1].xyz.normalized //* tangent1.length

        val Ct0 = frame0[2].xyz.normalized * tangent0.length
        val Ct1 = frame1[2].xyz.normalized * tangent1.length

        val dNt0 = dframe0[0].xyz
        val dNt1 = dframe1[0].xyz
        val dBt0 = dframe0[1].xyz
        val dBt1 = dframe1[1].xyz


        val at0 = scale0
        val at1 = scale1

        val dat0 = dscale0
        val dat1 = dscale1


        val dSds00 = (Nt0 * (ct0 * dps0.x - st0 * dps0.y) + Bt0 * (st0 * dps0.x + ct0 * dps0.y)) * at0 //.normalized
        val dSds01 = (Nt0 * (ct0 * dps1.x - st0 * dps1.y) + Bt0 * (st0 * dps1.x + ct0 * dps1.y)) * at0//.normalized
        val dSds10 = (Nt1 * (ct1 * dps0.x - st1 * dps0.y) + Bt1 * (st1 * dps0.x + ct1 * dps0.y)) * at1//.normalized
        val dSds11 = (Nt1 * (ct1 * dps1.x - st1 * dps1.y) + Bt1 * (st1 * dps1.x + ct1 * dps1.y))  * at1//.normalized

        val dpxdt_t0s0 = dat0 * (ct0 * ps0.x - st0 * ps0.y) + at0 * dtheta0 * (-st0 * ps0.x - ct0 * ps0.y)
        val dpydt_t0s0 = dat0 * (st0 * ps0.x + ct0 * ps0.y) + at0 * dtheta0 * (ct0 * ps0.x - st0 * ps0.y)

        val dpxdt_t1s0 = dat1 * (ct1 * ps0.x - st1 * ps0.y) + at1 * dtheta1 * (-st1 * ps0.x - ct1 * ps0.y)
        val dpydt_t1s0 = dat1 * (st1 * ps0.x + ct1 * ps0.y) + at1 * dtheta1 * (ct1 * ps0.x - st1 * ps0.y)

        val dpxdt_t0s1 = dat0 * (ct0 * ps1.x - st0 * ps1.y) + at0 * dtheta0 * (-st0 * ps1.x - ct0 * ps1.y)
        val dpydt_t0s1 = dat0 * (st0 * ps1.x + ct0 * ps1.y) + at0 * dtheta0 * (ct0 * ps1.x - st0 * ps1.y)

        val dpxdt_t1s1 = dat1 * (ct1 * ps1.x - st1 * ps1.y) + at1 * dtheta1 * (-st1 * ps1.x - ct1 * ps1.y)
        val dpydt_t1s1 = dat1 * (st1 * ps1.x + ct1 * ps1.y) + at1 * dtheta1 * (ct1 * ps1.x - st1 * ps1.y)

        val dSdt_t0s0 = Ct0 + Nt0 * dpxdt_t0s0 + dNt0 * ps0.x + Bt0 * dpydt_t0s0 + dBt0 * ps0.y
        val dSdt_t1s0 = Ct1 + Nt1 * dpxdt_t1s0 + dNt1 * ps0.x + Bt1 * dpydt_t1s0 + dBt1 * ps0.y
        val dSdt_t0s1 = Ct0 + Nt0 * dpxdt_t0s1 + dNt0 * ps1.x + Bt0 * dpydt_t0s1 + dBt0 * ps1.y
        val dSdt_t1s1 = Ct1 + Nt1 * dpxdt_t1s1 + dNt1 * ps1.x + Bt1 * dpydt_t1s1 + dBt1 * ps1.y

        val faceNormal = ((vt0s1 - vt0s0).normalized cross (vt1s0 - vt0s0).normalized).normalized
        val n00 = if (useFaceNormals) faceNormal else (dSds00 cross dSdt_t0s0).normalized
        val n01 = if (useFaceNormals) faceNormal else (dSds01 cross dSdt_t0s1).normalized

        val n10 = if (useFaceNormals) faceNormal else (dSds10 cross dSdt_t1s0).normalized
        val n11 = if (useFaceNormals) faceNormal else (dSds11 cross dSdt_t1s1).normalized

        quadToTris(
            vt0s0, vt0s1, vt1s0, vt1s1,
            n00, n01, n10, n11,
            writer
        )
    }
}

/**
 * Extrude a [contour] along a [path] specifying the number of steps.
 *
 * @param contour the cross-section of the mesh
 * @param path the 3D path
 * @param stepCount the number of steps along the [path]
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [contour]. Lower tolerance results in higher precision.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision.
 * @param steps the resulting positions in the path
 * @param frames a list of matrices holding the transformation matrices along
 * the path
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 * @param writer the vertex writer function
 */
fun extrudeContourSteps(
    contour: ShapeContour,
    path: Path3D,
    stepCount: Int,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    steps: List<Vector3> = path.equidistantPositions(
        stepCount,
        pathDistanceTolerance
    ),
    frames: List<Matrix44> = steps.frames(up0),
    startCap: Boolean = true,
    endCap: Boolean = true,
    useFaceNormals: Boolean,
    writer: VertexWriter
) {
    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    val linearContourPointsWithT = contour.adaptivePositionsWithT(contourDistanceTolerance)
    val linearContourPoints = linearContourPointsWithT.map { it.first }
    val linearContourNormals = linearContourPointsWithT.map { contour.normal(it.second) }
    val finalFrames = if (path.closed) frames + frames.first() else frames

    // First add caps
    extrudeCaps(linearContour.shape, path, startCap, endCap, frames, writer)

    // Then add sides
    finalFrames.windowed(2, 1).forEach {
        contourSegmentWithNormals(linearContourPoints, linearContourNormals, it[0], it[1], writer, useFaceNormals)
    }
}

typealias ScalarFunction = (Double) -> Double

private fun ScalarFunction.diff(t: Double, eps: Double = 1E-2): Double {
    return (this(t + eps) - this(t - eps)) / (2.0 * eps)
}

fun ShapeContour.derivative(ut: Double): Vector2 {

    val eps = 1e-2
    return (position(ut + eps) - position(ut-eps)) / (2.0 * eps) * 1.0
//    if (empty) {
//        return infinity
//    }
//
//    return when (val t = ut.coerceIn(0.0, 1.0)) {
//        0.0 -> segments[0].derivative(0.0)
//        1.0 -> segments.last().derivative(1.0)
//        else -> {
//            val (segment, segmentOffset) = segment(t)
//            segments[segment].derivative(segmentOffset)
//        }
//    }
}

fun extrudeContourStepsScaled(
    contour: ShapeContour,
    path: Path3D,
    scale: (Double) -> Double,
    torsion: (Double) -> Double,
    contourDistanceTolerance: Double = 0.5,
    frames: List<Matrix44>,
    dframes: List<Matrix44>,
    ts: DoubleArray,
    startCap: Boolean = true,
    endCap: Boolean = true,
    useFaceNormals: Boolean,
    writer: VertexWriter
) {

    require(frames.size == dframes.size) { "frames and dframes must have the same size" }
    val linearContourPointsWithT = contour.adaptivePositionsWithT(contourDistanceTolerance)
    val linearContourPoints = linearContourPointsWithT.map { it.first }
    val linearContourNormals = linearContourPointsWithT.map { contour.derivative(it.second) }
    val finalFrames = if (path.closed) frames + frames.first() else frames

    // First add caps
    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    extrudeCaps(linearContour.shape, path, startCap, endCap, frames, writer)

    // Then add sides
    //finalFrames.windowed(2, 1).forEach {
    val eps = 1E-2
    for (i in 0 until ts.size - 1) {
        val frame0 = frames[i]
        val frame1 = frames[i + 1]
        val dframe0 = dframes[i]
        val dframe1 = dframes[i + 1]

        val scale0 = scale(ts[i])
        val scale1 = scale(ts[i + 1])
        val dscale0 = scale.diff(ts[i], eps)*1.0 //scale(ts[i] + eps) - scale(ts[i] - eps)) / (2 * eps)
        val dscale1 = scale.diff(ts[i + 1], eps)*1.0
        val tangent0 = (path.position(ts[i] + eps) - path.position(ts[i] - eps)) / (2 * eps)
        val tangent1 = (path.position(ts[i + 1] + eps) - path.position(ts[i + 1] - eps)) / (2 * eps)

        val torsion0 = torsion(ts[i])
        val torsion1 = torsion(ts[i + 1])
        val dtorsion0 = torsion.diff(ts[i], eps)
        val dtorsion1 = torsion.diff(ts[i + 1], eps)

        contourSegmentWithNormals(
            linearContourPoints,
            linearContourNormals,
            scale0, scale1, dscale0, dscale1,
            torsion0, torsion1, dtorsion0, dtorsion1,
            frame0, frame1, dframe0, dframe1,
            tangent0,
            tangent1,
            writer,
            useFaceNormals = useFaceNormals
        )

    }
    //}
}

/**
 * Extrude a [contour] along a [path]. The number of resulting steps
 * along the path depends on the tolerance values.
 *
 * @param contour the cross-section of the mesh
 * @param path the 3D path
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [contour]. Lower tolerance results in higher precision and step count.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision and step count.
 * @param steps the resulting positions in the path
 * @param frames a list of matrices holding the transformation matrices along
 * the path
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 * @param writer the vertex writer function
 */
fun extrudeContourAdaptive(
    contour: ShapeContour,
    path: Path3D,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    steps: List<Vector3> = path.adaptivePositions(pathDistanceTolerance),
    frames: List<Matrix44> = steps.frames(up0),
    startCap: Boolean = true,
    endCap: Boolean = true,
    useFaceNormals: Boolean,
    writer: VertexWriter
) {
    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    val linearContourPointsWithT = contour.adaptivePositionsWithT(contourDistanceTolerance)
    val linearContourPoints = linearContourPointsWithT.map { it.first }
    val linearContourNormals = linearContourPointsWithT.map { contour.normal(it.second) }

    val finalFrames = if (path.closed) frames + frames.first() else frames

    // First add caps
    extrudeCaps(linearContour.shape, path, startCap, endCap, finalFrames, writer)

    // Then add sides
    finalFrames.windowed(2, 1).forEach {
        contourSegmentWithNormals(linearContourPoints, linearContourNormals, it[0], it[1], writer, useFaceNormals)
    }
}

/**
 * Extrude a [shape] along a [path] specifying the number of steps.
 *
 * @param shape the cross-section of the mesh
 * @param path the 3D path
 * @param stepCount the number of steps along the [path]
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [shape]. Lower tolerance results in higher precision.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision.
 * @param steps the resulting positions in the path
 * @param frames a list of matrices holding the transformation matrices along
 * the path
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 * @param writer the vertex writer function
 */
fun extrudeShapeSteps(
    shape: Shape,
    path: Path3D,
    stepCount: Int,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    steps: List<Vector3> = path.equidistantPositions(stepCount, pathDistanceTolerance),
    frames: List<Matrix44> = steps.frames(up0),
    startCap: Boolean,
    endCap: Boolean,
    useFaceNormals: Boolean,
    writer: VertexWriter
) {
    val linearShape = Shape(shape.contours.map { it.contour.sampleLinear(contourDistanceTolerance) })

    // First add caps
    extrudeCaps(linearShape, path, startCap, endCap, frames, writer)

    // Then add sides
    for (contour in linearShape.contours) {
        extrudeContourSteps(
            contour,
            path,
            stepCount,
            up0,
            contourDistanceTolerance,
            pathDistanceTolerance,
            steps,
            frames,
            startCap = false,
            endCap = false,
            useFaceNormals,
            writer
        )
    }
}

fun TriangleMeshBuilder.extrudeShape(
    shape: Shape, path: Path3D,
    scale: (Double) -> Double,
    torsion: (Double) -> Double,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    startCap: Boolean = true,
    endCap: Boolean = true,
    useFaceNormals: Boolean = false

) {
    extrudeShape(
        shape,
        path,
        scale,
        torsion,
        up0,
        contourDistanceTolerance,
        pathDistanceTolerance,
        startCap,
        endCap,
        useFaceNormals = useFaceNormals,

        this::write
    )
}

fun extrudeShape(
    shape: Shape,
    path: Path3D,
    scale: (Double) -> Double,
    torsion: (Double) -> Double,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    startCap: Boolean,
    endCap: Boolean,
    useFaceNormals: Boolean,
    writer: VertexWriter
) {
    val linearShape = Shape(shape.contours.map { it.contour.sampleLinear(contourDistanceTolerance) })

    val rectified = path.rectified(pathDistanceTolerance, 1.0)
    val pose = rectified.pose(up0)

    val frames = rectified.points.map { pose.pose(it.second) }
    val dframes = rectified.points.map { pose.poseDerivative((it.second)) }

    // First add caps
    extrudeCaps(linearShape, path, startCap, endCap, frames, writer)

    // Then add sides
    for (contour in linearShape.contours) {
        extrudeContourStepsScaled(
            contour,
            path,
            scale,
            torsion,
            contourDistanceTolerance,
            frames,
            dframes,
            rectified.points.map { it.second }.toDoubleArray(),
            startCap = false,
            endCap = false,
            useFaceNormals = useFaceNormals,
            writer
        )
    }
}

/**
 * Extrude a [shape] along a [path]. The number of resulting steps
 * along the path depends on the tolerance values.
 *
 * @param shape the cross-section of the mesh
 * @param path the 3D path
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [shape]. Lower tolerance results in higher precision and step count.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision and step count.
 * @param steps the resulting positions in the path
 * @param frames a list of matrices holding the transformation matrices along
 * the path
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 * @param writer the vertex writer function
 */
fun extrudeShapeAdaptive(
    shape: Shape,
    path: Path3D,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    steps: List<Vector3> = path.adaptivePositions(pathDistanceTolerance),
    frames: List<Matrix44> = steps.frames(up0),
    startCap: Boolean,
    endCap: Boolean,
    useFaceNormals: Boolean,
    writer: VertexWriter
) {
    val linearShape = Shape(shape.contours.map { it.contour.sampleLinear(contourDistanceTolerance) })

    // First add caps
    extrudeCaps(linearShape, path, startCap, endCap, frames, writer)

    // Then add sides
    for (contour in linearShape.contours) {
        extrudeContourAdaptive(
            contour,
            path,
            up0,
            contourDistanceTolerance,
            pathDistanceTolerance,
            steps,
            frames,
            startCap,
            endCap,
            useFaceNormals,
            writer
        )
    }
}

/**
 * Extrude a [shape] along a [path] specifying the number of steps.
 *
 * @param shape the cross-section of the mesh
 * @param path the 3D path
 * @param stepCount the number of steps along the [path]
 * @param up0 the up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [shape]. Lower tolerance results in higher precision.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision.
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 */
fun TriangleMeshBuilder.extrudeShapeSteps(
    shape: Shape,
    path: Path3D,
    stepCount: Int,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    startCap: Boolean = true,
    endCap: Boolean = true,
    useFaceNormals: Boolean = false
) = extrudeShapeSteps(
    shape,
    path,
    stepCount,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    startCap = startCap,
    endCap = endCap,
    useFaceNormals = useFaceNormals,
    writer = this::write
)

/**
 * Extrude a [shape] along a [path]. The number of resulting steps
 * along the path depends on the tolerance values.
 *
 * @param shape the cross-section of the mesh
 * @param path the 3D path
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [shape]. Lower tolerance results in higher precision and step count.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision and step count.
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 */
fun TriangleMeshBuilder.extrudeShapeAdaptive(
    shape: Shape,
    path: Path3D,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    startCap: Boolean = true,
    endCap: Boolean = true,
    useFaceNormals: Boolean = false
) = extrudeShapeAdaptive(
    shape,
    path,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    startCap = startCap,
    endCap = endCap,
    useFaceNormals = useFaceNormals,
    writer = this::write
)

/**
 * Extrude a [contour] along a [path] specifying the number of steps.
 *
 * @param contour the cross-section of the mesh
 * @param path the 3D path
 * @param stepCount the number of steps along the [path]
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [contour]. Lower tolerance results in higher precision.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision.
 */
fun TriangleMeshBuilder.extrudeContourSteps(
    contour: ShapeContour,
    path: Path3D,
    stepCount: Int,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    useFaceNormals: Boolean = false,
) = extrudeContourSteps(
    contour,
    path,
    stepCount,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    writer = this::write,
    useFaceNormals = useFaceNormals
)

/**
 * Extrude a [contour] along a [path]. The number of resulting steps
 * along the path depends on the tolerance values.
 *
 * @param contour the cross-section of the shape
 * @param path the 3D path
 * @param up0 the up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [contour]. Lower tolerance results in higher precision and step count.
 * @param pathDistanceTolerance precision for calculating steps along
 * [path]. Lower tolerance results in higher precision and step count.
 */
fun TriangleMeshBuilder.extrudeContourAdaptive(
    contour: ShapeContour,
    path: Path3D,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    useFaceNormals: Boolean = false
) = extrudeContourAdaptive(
    contour,
    path,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    writer = this::write, useFaceNormals = useFaceNormals
)
