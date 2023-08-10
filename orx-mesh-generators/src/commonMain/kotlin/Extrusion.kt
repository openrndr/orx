package org.openrndr.extra.meshgenerators

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.shape.Path3D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.Triangle

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
fun contourSegment(
    linearContour: List<Vector2>,
    frame0: Matrix44,
    frame1: Matrix44,
    writer: VertexWriter
) {
    for (i in linearContour.indices) {
        val v0 = linearContour[i]
        val v1 = linearContour[(i + 1).mod(linearContour.size)]

        val v00 = (frame0 * v0.xy01).xyz
        val v01 = (frame0 * v1.xy01).xyz

        val v10 = (frame1 * v0.xy01).xyz
        val v11 = (frame1 * v1.xy01).xyz
        val faceNormal = ((v10 - v00).normalized cross (v01 - v00).normalized).normalized
        quadToTris(v00, v01, v10, v11, faceNormal, writer)
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
    writer: VertexWriter
) {
    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    val linearContourPoints = linearContour.adaptivePositions()
    val finalFrames = if (path.closed) frames + frames.first() else frames

    // First add caps
    extrudeCaps(linearContour.shape, path, startCap, endCap, frames, writer)

    // Then add sides
    finalFrames.windowed(2, 1).forEach {
        contourSegment(linearContourPoints, it[0], it[1], writer)
    }
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
    writer: VertexWriter
) {
    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    val linearContourPoints = linearContour.adaptivePositions()
    val finalFrames = if (path.closed) frames + frames.first() else frames

    // First add caps
    extrudeCaps(linearContour.shape, path, startCap, endCap, finalFrames, writer)

    // Then add sides
    finalFrames.windowed(2, 1).forEach {
        contourSegment(linearContourPoints, it[0], it[1], writer)
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
) = extrudeShapeSteps(
    shape,
    path,
    stepCount,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    startCap = startCap,
    endCap = endCap,
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
    endCap: Boolean = true
) = extrudeShapeAdaptive(
    shape,
    path,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    startCap = startCap,
    endCap = endCap,
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
) = extrudeContourSteps(
    contour,
    path,
    stepCount,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    writer = this::write
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
    pathDistanceTolerance: Double = 0.5
) = extrudeContourAdaptive(
    contour,
    path,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    writer = this::write
)
