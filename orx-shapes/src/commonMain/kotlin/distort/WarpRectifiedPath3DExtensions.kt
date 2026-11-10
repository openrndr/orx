package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.pose.pose
import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D

/**
 * Warps this path from following [basis] to following [warp]: the 3D counterpart of
 * [org.openrndr.extra.shapes.rectify.RectifiedContour.warp].
 *
 * Every point of this path is found by its nearest point on [basis]; its offset from there is measured in the frame of
 * the basis at that point, across the basis and along it, and the point is placed at the same offset in the frame of
 * [warp] at the same rectified t. The frames are those of [org.openrndr.extra.shapes.pose.PosePath3D], starting with
 * their y axis along [up], so that a path that winds around the basis winds the same way around the warp.
 *
 * @param up the up of the first frames of both paths
 * @param errorTolerance maximum RMS error of the cubic pieces of the result, see [RectifiedPath3D.distort]
 * @param maxDepth recursion limit of the splitting of pieces, see [RectifiedPath3D.distort]
 * @param samplesPerPiece samples used to fit and measure every piece, see [RectifiedPath3D.distort]
 * @return the warped path
 */
fun RectifiedPath3D.warp(
    basis: RectifiedPath3D,
    warp: RectifiedPath3D,
    up: Vector3 = Vector3.UNIT_Y,
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
): Path3D {
    val basisPoses = basis.pose(up)
    val warpPoses = warp.pose(up)
    return distort(errorTolerance, maxDepth, samplesPerPiece) { _, position ->
        val t = basis.nearest(position)
        val from = basisPoses.pose(t)
        val to = warpPoses.pose(t)
        // the offset in the frame of the basis: across it in x and y, along it in z (beyond the ends of an open basis)
        val d = position - Vector3(from.c3r0, from.c3r1, from.c3r2)
        val x = d.dot(Vector3(from.c0r0, from.c0r1, from.c0r2))
        val y = d.dot(Vector3(from.c1r0, from.c1r1, from.c1r2))
        val z = d.dot(Vector3(from.c2r0, from.c2r1, from.c2r2))
        Vector3(to.c3r0, to.c3r1, to.c3r2) +
                Vector3(to.c0r0, to.c0r1, to.c0r2) * x +
                Vector3(to.c1r0, to.c1r1, to.c1r2) * y +
                Vector3(to.c2r0, to.c2r1, to.c2r2) * z
    }
}
