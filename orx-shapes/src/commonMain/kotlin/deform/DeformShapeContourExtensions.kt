package org.openrndr.extra.shapes.deform

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * Deforms the current ShapeContour by applying an attraction force toward a given attractor point.
 * The deformation is influenced by various parameters including strength, falloff, and reference distance,
 * and adaptively refines the contour where necessary to meet an error tolerance.
 *
 * @param attractor The vector representing the point toward which the contour is attracted.
 * @param strength The magnitude of the attraction force, where higher values result in greater deformation.
 * @param falloff The exponent controlling how the attraction force decreases with distance from the attractor.
 * @param refDistance A reference distance that normalizes the falloff effect, preventing excessive influence near the attractor.
 * @param errorTolerance The maximum allowed root mean square (RMS) error for approximating the deformed contour. Default is 0.5.
 * @param maxDepth The maximum recursion depth for refining the contour, limiting the number of pieces in the output. Default is 6.
 * @param samplesPerPiece The number of interior sample points used for both fitting and measuring deformation errors. Default is 24.
 * @return A new ShapeContour representing the deformed version of the original.
 */
fun ShapeContour.deform(
    attractor: Vector2, strength: Double, falloff: Double, refDistance: Double,
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24
): ShapeContour {

    val newSegments = segments.flatMap {
        it.deform(attractor, strength, falloff, refDistance, 0.0, errorTolerance, maxDepth, samplesPerPiece)
    }

    return ShapeContour.fromSegments(newSegments, closed)
}
