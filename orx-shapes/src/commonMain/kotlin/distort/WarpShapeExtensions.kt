package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Shape

/**
 * Warps the shape by applying a transformation based on provided basis and warp contours.
 * Each contour within the shape undergoes the transformation process to produce a new warped shape.
 *
 * @param errorTolerance The maximum allowed RMS fit error used during the distortion process. Default is 0.5.
 * @param maxDepth The maximum recursion depth for adaptive piece splitting during distortion. Default is 6.
 * @param samplesPerPiece The number of samples per piece used for fitting and error measurement. Default is 24.
 * @param basis The reference contour used for calculating nearest positions and normals during the transformation.
 * @param warp The source contour used to determine displacement and transformation effects on the shape contours.
 * @return A new Shape object containing the warped contours after applying the transformation process.
 */
fun Shape.warp(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    basis: RectifiedContour, warp: RectifiedContour): Shape {
    val rectified = shape.contours.map { it.rectified() }
    return Shape(rectified.map { it.warp(basis, warp, errorTolerance, maxDepth, samplesPerPiece) })
}