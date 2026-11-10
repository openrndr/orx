package org.openrndr.extra.shapes.distort

import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D

/**
 * Distorts the points along this [Path3D] by applying a specified distortion function: the 3D counterpart of
 * [org.openrndr.shape.ShapeContour.distort]. The output is a new [Path3D] with segments that adaptively approximate
 * the distorted curve while keeping the original closed property.
 *
 * The distortion function is applied independently to each point of the path, and the distorted points are
 * approximated by cubic segments, split adaptively based on the error tolerance and depth limit. Every piece starts
 * and ends at the distorted ends of its part of the path, so pieces join exactly.
 *
 * @param errorTolerance The maximum allowed RMS error (in the same units as the path's coordinates) before a segment
 *                       is subdivided. Higher values result in less precise approximations but faster processing.
 * @param maxDepth       The recursion depth limit for splitting segments. Limits the number of pieces per segment to
 *                       2^maxDepth. Recommended range is 5 to 8.
 * @param samplesPerPiece The number of interior samples used for measuring fit error and curve fitting.
 *                        Larger values increase accuracy at the cost of computational performance.
 * @param distort        A function that maps an input [Vector3] (position) to a distorted [Vector3] position.
 * @return A new [Path3D] resulting from the adaptive distortion of this path using the provided distortion function.
 */
fun Path3D.distort(
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24,
    distort: (Vector3) -> Vector3
): Path3D {
    return Path3D(
        segments.flatMap { it.distort(errorTolerance, maxDepth, samplesPerPiece, distort) },
        closed
    )
}
