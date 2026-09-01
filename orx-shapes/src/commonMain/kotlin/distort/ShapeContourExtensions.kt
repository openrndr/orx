package org.openrndr.extra.shapes.distort

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour


/**
 * Distorts the points along this [ShapeContour] by applying a specified distortion function.
 * The output is a new [ShapeContour] with segments that adaptively approximate the distorted
 * curve while maintaining the original closed property.
 *
 * The distortion function is applied independently to each point in the input contour,
 * and the resulting distorted points are used to create an approximated curve by splitting
 * and fitting segments adaptively based on the specified error tolerance and depth limits.
 *
 * @param errorTolerance The maximum allowed RMS error (in the same units as the curve's coordinates)
 *                       before a segment is subdivided. Higher values result in less precise
 *                       approximations but faster processing.
 * @param maxDepth       The recursion depth limit for splitting segments. Limits the number
 *                       of resulting pieces to 2^maxDepth. Recommended range is 5 to 8.
 * @param samplesPerPiece The number of interior samples used for measuring fit error and curve fitting.
 *                        Larger values increase accuracy at the cost of computational performance.
 * @param distort        A function that maps an input [Vector2] (position) to a distorted
 *                       [Vector2] position. This function defines the type of distortion
 *                       (e.g., noise, swirls, attraction) applied.
 * @return A new [ShapeContour] resulting from the adaptive distortion of this contour
 *         using the provided distortion function.
 */
fun ShapeContour.distort(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    distort: (Vector2) -> Vector2
): ShapeContour {

    return ShapeContour(
    segments.flatMap { it.distort(errorTolerance, maxDepth, samplesPerPiece, distort) },
        closed
    )
}