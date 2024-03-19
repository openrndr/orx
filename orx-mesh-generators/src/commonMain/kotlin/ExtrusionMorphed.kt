package org.openrndr.extra.meshgenerators

import org.openrndr.extra.shapes.frames.frames
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.ShapeContour

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
fun extrudeContourStepsMorphed(
    contour: (Double) -> ShapeContour,
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
    val finalFrames = if (path.closed) frames + frames.first() else frames

    val crossSections = List(finalFrames.size) {
        val t = it / (finalFrames.size - 1.0)
        val linearContour = contour(t).sampleLinear(contourDistanceTolerance)
        linearContour.adaptivePositions()
    }

    // Add caps
    if (!path.closed) {
        if (startCap) {
            triangulationWithFrame(
                ShapeContour.fromPoints(crossSections.first(), true).shape.triangulation,
                finalFrames.first(), false, writer
            )
        }
        if (endCap) {
            triangulationWithFrame(
                ShapeContour.fromPoints(crossSections.last(), true).shape.triangulation,
                finalFrames.last(), false, writer
            )
        }
    }

    // Add sides
    var i = 0
    (finalFrames zip crossSections).windowed(2, 1).forEach {
        contourSegment(
            it[0].second, it[1].second,
            it[0].first, it[1].first,
            i.toDouble() / crossSections.size,
            (++i).toDouble() / crossSections.size,
            writer)
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
 */
fun TriangleMeshBuilder.extrudeContourStepsMorphed(
    contour: (Double) -> ShapeContour,
    path: Path3D,
    stepCount: Int,
    up0: Vector3,
    startCap: Boolean = true,
    endCap: Boolean = true,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5
) = extrudeContourStepsMorphed(
    contour,
    path,
    stepCount,
    up0,
    startCap = startCap,
    endCap = endCap,
    contourDistanceTolerance = contourDistanceTolerance,
    pathDistanceTolerance = pathDistanceTolerance,
    writer = this::write
)

