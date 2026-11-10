package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import org.openrndr.shape.Path3D
import org.openrndr.shape.Rectangle

/**
 * Distorts this [Path3D] by placing its points in a [target] Bezier volume, by their places in the [basis] box: a point
 * at the corner of the box goes to the corner of the volume where u, v and w are 0, a point at the opposite corner to
 * where they are 1. This is free-form deformation: a volume made with [BezierVolume.fromBox] leaves the path as it is,
 * moving its control points bends, twists or tapers the path.
 *
 * @param errorTolerance maximum RMS error of the cubic pieces of the result, see [Path3D.distort]
 * @param maxDepth recursion limit of the splitting of pieces, see [Path3D.distort]
 * @param samplesPerPiece samples used to fit and measure every piece, see [Path3D.distort]
 */
fun Path3D.envelope(
    basis: Box,
    target: BezierVolume,
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24
): Path3D = distort(errorTolerance, maxDepth, samplesPerPiece) { p ->
    val uvw = (p - basis.corner) / Vector3(basis.width, basis.height, basis.depth)
    target.position(uvw.x, uvw.y, uvw.z)
}

/**
 * Distorts this [Path3D] by laying it onto a [target] Bezier patch: the x and y of a point in the [basis] rectangle
 * give its u and v on the patch, and its z its distance from the patch along the patch's normal. A path in the plane
 * of z = 0 lies on the surface; this is the 3D counterpart of [org.openrndr.shape.ShapeContour.envelope].
 *
 * @param errorTolerance maximum RMS error of the cubic pieces of the result, see [Path3D.distort]
 * @param maxDepth recursion limit of the splitting of pieces, see [Path3D.distort]
 * @param samplesPerPiece samples used to fit and measure every piece, see [Path3D.distort]
 */
fun Path3D.envelope(
    basis: Rectangle,
    target: BezierPatch3D,
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24
): Path3D = distort(errorTolerance, maxDepth, samplesPerPiece) { p ->
    val u = (p.x - basis.corner.x) / basis.width
    val v = (p.y - basis.corner.y) / basis.height
    target.position(u, v) + target.normal(u, v) * p.z
}
