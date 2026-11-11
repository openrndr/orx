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
 * @param pathDistanceTolerance no longer used: the steps are at equal distances along [path]
 * @param steps the resulting positions in the path
 * @param frames a list of matrices holding the transformation matrices along
 * the path
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 * @param scale A function that takes the part of the steps, from 0 at the first to 1 at the last, and returns
 * a scaling factor for [contour] at that step. Where it is 0 the cross-section is a point, and a cap there is left out.
 * @param useFaceNormals true for the normals of the faces, false for smooth normals that follow the changes of scale
 * @param writer the vertex writer function
 */
fun extrudeContourStepsScaled(
    contour: ShapeContour,
    path: Path3D,
    stepCount: Int,
    up0: Vector3,
    contourDistanceTolerance: Double = 0.5,
    pathDistanceTolerance: Double = 0.5,
    steps: List<Vector3> = path.evenSteps(stepCount),
    frames: List<Matrix44> = path.posesAt(steps, up0),
    startCap: Boolean = true,
    endCap: Boolean = true,
    scale: (Double) -> Double = { _ -> 1.0 },
    useFaceNormals: Boolean,
    writer: VertexWriter
) {
    val loops = crossSectionLoops(listOf(contour), contourDistanceTolerance)
    val frameRows = frameRows(frames, path.closed)
    // every step scaled by the part of the steps it is at
    val rows = frameRows.mapIndexed { i, row ->
        val s = scale(if (frameRows.size < 2) 0.0 else i / (frameRows.size - 1.0))
        ExtrusionRow(row.origin, row.x * s, row.y * s, row.z, row.normalX, row.normalY)
    }
    extrudeRowLoops({ loops }, rows, path.closed, startCap, endCap, useFaceNormals, true, writer)
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
    scale: (Double) -> Double = { _ -> 1.0 },
    useFaceNormals: Boolean = false
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
    writer = this::write,
    useFaceNormals =  useFaceNormals
)

