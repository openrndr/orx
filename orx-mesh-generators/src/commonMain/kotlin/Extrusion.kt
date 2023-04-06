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
 * Writes two triangles to [writer] representing
 * the quad formed by four vertices.
 *
 * @param v00 vertex (0, 0)
 * @param v01 vertex (0, 1)
 * @param v10 vertex (1, 0)
 * @param v11 vertex (1, 1)
 * @param faceNormal the face normal
 * @param writer the vertex writer function
 */
fun quadToTris(
    v00: Vector3,
    v01: Vector3,
    v10: Vector3,
    v11: Vector3,
    faceNormal: Vector3,
    writer: VertexWriter
) {
    writer(v11, faceNormal, Vector2.ZERO)
    writer(v01, faceNormal, Vector2.ZERO)
    writer(v00, faceNormal, Vector2.ZERO)

    writer(v00, faceNormal, Vector2.ZERO)
    writer(v10, faceNormal, Vector2.ZERO)
    writer(v11, faceNormal, Vector2.ZERO)
}

/**
 * Writes quads to [writer] creating a surface that connects two
 * displaced instances of [linearContour]. The positions and orientations
 * of the two contours are defined by [frame0] and [frame1].
 *
 * @param linearContour the cross-section of the surface to create
 * @param frame0 a transformation matrix that defines an initial position
 * @param frame1 a transformation matrix that defines a final position
 * @param writer the vertex writer function
 */
fun contourSegment(
    linearContour: List<Vector3>,
    frame0: Matrix44,
    frame1: Matrix44,
    writer: VertexWriter
) {
    for (i in linearContour.indices) {
        val v0 = linearContour[i]
        val v1 = linearContour[(i + 1).mod(linearContour.size)]

        val v00 = (frame0 * v0.xyz1).xyz
        val v01 = (frame0 * v1.xyz1).xyz

        val v10 = (frame1 * v0.xyz1).xyz
        val v11 = (frame1 * v1.xyz1).xyz
        val faceNormal = ((v10 - v00).normalized cross (v01 - v00).normalized).normalized
        quadToTris(v00, v01, v10, v11, faceNormal, writer)
    }
}

/**
 * Writes a list of triangles transformed by the [frame]
 * transformation matrix into [writer].
 *
 * @param triangulation the list of triangles to write
 * @param frame a transformation matrix to apply to each triangle
 * @param flipNormals generates inside-out geometry if true
 * @param writer the vertex writer function
 */
fun triangulationWithFrame(
    triangulation: List<Triangle>,
    frame: Matrix44,
    flipNormals: Boolean = true,
    writer: VertexWriter
) {
    val normalFrame = normalMatrix(frame)
    val normalScale = if (!flipNormals) -1.0 else 1.0
    val normal = ((normalFrame * Vector4(0.0, 0.0, normalScale, 0.0)).xyz)
    for (triangle in triangulation) {
        val t = if (!flipNormals) triangle else Triangle(triangle.x3, triangle.x2, triangle.x1)
        writer((frame * t.x1.xy01).xyz, normal, Vector2.ZERO)
        writer((frame * t.x2.xy01).xyz, normal, Vector2.ZERO)
        writer((frame * t.x3.xy01).xyz, normal, Vector2.ZERO)
    }
}

/**
 * Extrude contour steps
 *
 * @param contour
 * @param path
 * @param stepCount
 * @param up0
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
 * @param steps
 * @param frames
 * @param startCap
 * @param endCap
 * @param writer
 */
fun extrudeContourSteps(
    contour: ShapeContour, path: Path3D, stepCount: Int, up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    steps: List<Vector3> = path.equidistantPositions(stepCount, pathDistanceTolerance),
    frames: List<Matrix44> = steps.frames(up0),
    startCap: Boolean = true,
    endCap: Boolean = true,
    writer: VertexWriter
) {
    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    val linearContourPoints = linearContour.adaptivePositions().map { it.xy0 }

    extrudeCaps(linearContour.shape, path, startCap, endCap, frames, writer)
    val finalFrames = if (path.closed) frames + frames.first() else frames
    finalFrames.windowed(2, 1).forEach {
        contourSegment(linearContourPoints, it[0], it[1], writer)
    }
}

/**
 * Extrude caps
 *
 * @param linearShape
 * @param path
 * @param startCap
 * @param endCap
 * @param frames
 * @param writer
 */
private fun extrudeCaps(
    linearShape: Shape,
    path: Path3D,
    startCap: Boolean,
    endCap: Boolean,
    frames: List<Matrix44>,
    writer: VertexWriter
) {
    if ((startCap || endCap) && !path.closed) {
        val capTriangles = linearShape.triangulation
        if (startCap) {
            triangulationWithFrame(capTriangles, frames.first(), false, writer)
        }
        if (endCap) {
            triangulationWithFrame(capTriangles, frames.last(), true, writer)
        }
    }
}

/**
 * Extrude contour adaptive
 *
 * @param contour
 * @param path
 * @param up0
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
 * @param steps
 * @param frames
 * @param startCap
 * @param endCap
 * @param writer
 */
fun extrudeContourAdaptive(
    contour: ShapeContour, path: Path3D, up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    steps: List<Vector3> = path.adaptivePositions(pathDistanceTolerance),
    frames: List<Matrix44> = steps.frames(up0),
    startCap: Boolean = true,
    endCap: Boolean = true,
    writer: VertexWriter
) {
    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    val linearContourPoints = linearContour.adaptivePositions().map { it.xy0 }
    val finalFrames = if (path.closed) frames + frames.first() else frames
    extrudeCaps(linearContour.shape, path, startCap, endCap, finalFrames, writer)
    finalFrames.windowed(2, 1).forEach {
        contourSegment(linearContourPoints, it[0], it[1], writer)
    }
}

/**
 * Extrude shape steps
 *
 * @param shape
 * @param path
 * @param stepCount
 * @param up0
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
 * @param steps
 * @param frames
 * @param startCap
 * @param endCap
 * @param writer
 */
fun extrudeShapeSteps(
    shape: Shape,
    path: Path3D, stepCount: Int,
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
    extrudeCaps(linearShape, path, startCap, endCap, frames, writer)

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
            false,
            false,
            writer
        )
    }
}

/**
 * Extrude shape adaptive
 *
 * @param shape
 * @param path
 * @param up0
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
 * @param steps
 * @param frames
 * @param startCap
 * @param endCap
 * @param writer
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
    extrudeCaps(linearShape, path, startCap, endCap, frames, writer)

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
 * Extrude shape steps
 *
 * @param shape
 * @param path
 * @param stepCount
 * @param up0
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
 * @param startCap
 * @param endCap
 */
fun TriangleMeshBuilder.extrudeShapeSteps(
    shape: Shape,
    path: Path3D, stepCount: Int,
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
    contourDistanceTolerance = contourDistanceTolerance,
    pathDistanceTolerance = pathDistanceTolerance,
    startCap = startCap,
    endCap = endCap,
    writer = this::write
)

/**
 * Extrude shape adaptive
 *
 * @param shape
 * @param path
 * @param up0
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
 * @param startCap
 * @param endCap
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
    contourDistanceTolerance = contourDistanceTolerance,
    pathDistanceTolerance = pathDistanceTolerance,
    startCap = startCap,
    endCap = endCap,
    writer = this::write
)

/**
 * Extrude a [contour] along a [path] specifying the number of steps.
 *
 * @param contour the cross-section of the produced shape
 * @param path the 3D path
 * @param stepCount the number of steps along the [path]
 * @param up0 the up-vector
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
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
    contourDistanceTolerance = contourDistanceTolerance,
    pathDistanceTolerance = pathDistanceTolerance,
    writer = this::write
)

/**
 * Extrude a [contour] along a [path]. The number of resulting steps
 * along the path will depend on the tolerance values.
 *
 * @param contour the cross-section of the produced shape
 * @param path the 3D path
 * @param up0 the up-vector
 * @param contourDistanceTolerance
 * @param pathDistanceTolerance
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
    contourDistanceTolerance = contourDistanceTolerance,
    pathDistanceTolerance = pathDistanceTolerance,
    writer = this::write
)