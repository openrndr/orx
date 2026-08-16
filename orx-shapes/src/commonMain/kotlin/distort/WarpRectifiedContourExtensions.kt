package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.shape.ShapeContour
import kotlin.math.sign

/**
 * Warps the current contour by applying a distortion based on the provided basis and warp contours.
 * The operation computes a mapping where the nearest position and normal of the basis contour
 * are used to calculate the displacement applied to the warp contour, creating a warped result.
 *
 * @param basis The contour used as the reference for the nearest position and normal calculation.
 * @param warp The contour used as the source for applying displacement based on the mapped positions and normals.
 * @param errorTolerance Maximum allowed RMS fit error used during the distortion process. Default is 0.5.
 * @param maxDepth Maximum recursion depth for adaptive piece splitting during distortion. Default is 6.
 * @param samplesPerPiece Number of samples per piece used for fitting and error measurement. Default is 24.
 * @return A new warped ShapeContour created by distorting the current contour using the provided basis and warp contours.
 */
fun RectifiedContour.warp(
    basis: RectifiedContour,
    warp: RectifiedContour,
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,

    ): ShapeContour {
    return this.distort(errorTolerance, maxDepth, samplesPerPiece) { t, position ->
        val nbt = basis.nearest(position)
        val np = basis.position(nbt)
        val nbd = np.distanceTo(position)
        val side = basis.normal(nbt).dot(position - np).sign
        warp.position(nbt) + warp.normal(nbt) * side * nbd
    }
}