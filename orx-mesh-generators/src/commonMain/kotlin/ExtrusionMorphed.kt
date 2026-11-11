package org.openrndr.extra.meshgenerators

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.ShapeContour

/**
 * Extrude a [contour] along a [path] specifying the number of steps.
 *
 * @param contour the cross-section of the mesh at the part of the steps, from 0 at the first to 1 at the last; every
 * cross-section is sampled at as many points, at equal distances along it from its start, as the most detailed one
 * needs at [contourDistanceTolerance], and the points of neighbouring steps are joined in order
 * @param path the 3D path
 * @param stepCount the number of steps along the [path]
 * @param up0 the initial up-vector
 * @param contourDistanceTolerance precision for calculating steps along
 * [contour]. Lower tolerance results in higher precision.
 * @param pathDistanceTolerance no longer used: the steps are at equal distances along [path]
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
    steps: List<Vector3> = path.evenSteps(stepCount),
    frames: List<Matrix44> = path.posesAt(steps, up0),
    startCap: Boolean = true,
    endCap: Boolean = true,
    writer: VertexWriter
) {
    val rows = frameRows(frames, path.closed)
    // the cross-sections, all through as many points at equal distances along them as the most detailed one needs
    val contours = List(rows.size) { contour(if (rows.size < 2) 0.0 else it / (rows.size - 1.0)) }
    val count = contours.maxOf { c ->
        c.adaptivePositions(contourDistanceTolerance).size.let { if (c.closed) it - 1 else it }
    }
    val loops = contours.map { c -> listOf(loopOf(c, evenSamples(c, count))) }
    extrudeRowLoops({ loops[it] }, rows, path.closed, startCap, endCap, false, true, writer)
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

