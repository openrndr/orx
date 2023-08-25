package org.openrndr.extra.meshgenerators

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.ShapeContour

/**
 * Extrude a [contour] along a [path] specifying the number of steps.
 * The [scale] argument can be used to make variable width shapes.
 * For example `scale = { t: Double -> 0.5 - 0.5 * cos(t * 2 * PI) }`
 * produces an extruded shape that begins and ends with hairline thickness.
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
 * @param scale A function that takes a curve `t` value and returns
 * a scaling factor for [contour] at that point.
 * @param writer the vertex writer function
 */
fun extrudeContourStepsScaled(
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
    scale: (Double) -> Double = { _ -> 1.0 },
    writer: VertexWriter
) {
    val finalFrames = if (path.closed) frames + frames.first() else frames

    val linearContour = contour.sampleLinear(contourDistanceTolerance)
    val linearContourPoints2D = linearContour.adaptivePositions()

    val crossSections = List(finalFrames.size) {
        val t = it / (finalFrames.size - 1.0)
        linearContourPoints2D.map { p -> p * scale(t) }
    }
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

    // Then add sides
    (finalFrames zip crossSections).windowed(2, 1).forEach {
        contourSegment(it[0].second, it[1].second, it[0].first, it[1].first, writer)
    }
}

/**
 * Extrude a [contour] along a [path] specifying the number of steps.
 * The [scale] argument can be used to make variable width shapes.
 * For example `scale = { t: Double -> 0.5 - 0.5 * cos(t * 2 * PI) }`
 * produces an extruded shape that begins and ends with hairline thickness.
 *
 * @param contour the cross-section of the mesh
 * @param path the 3D path
 * @param stepCount the number of steps along the [path]
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [contour]. Lower tolerance results in higher precision.
 * @param pathDistanceTolerance precision for calculating steps along
 * @param scale A function that converts `t` into a radius
 * [path]. Lower tolerance results in higher precision.
 */
fun TriangleMeshBuilder.extrudeContourStepsScaled(
    contour: ShapeContour,
    path: Path3D,
    stepCount: Int,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    startCap: Boolean = true,
    endCap: Boolean = true,
    scale: (Double) -> Double = { _ -> 1.0 }
) = extrudeContourStepsScaled(
    contour,
    path,
    stepCount,
    up0,
    contourDistanceTolerance,
    pathDistanceTolerance,
    startCap = startCap,
    endCap = endCap,
    scale = scale,
    writer = this::write
)

