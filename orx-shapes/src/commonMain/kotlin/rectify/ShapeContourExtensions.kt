package org.openrndr.extra.shapes.rectify

import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour

/** create a rectified contour
 * @param distanceTolerance no longer used
 * @param lengthScale the number of points per unit of length in [RectifiedPath.points], default value is 1.0
 * @param lengthTolerance the largest error of the lengths along the path, in its units, default value is 1E-3
 **/
fun ShapeContour.rectified(
    distanceTolerance: Double = 0.5,
    lengthScale: Double = 1.0,
    lengthTolerance: Double = 1E-3
): RectifiedContour {
    return RectifiedContour(this, distanceTolerance, lengthScale, lengthTolerance)
}

/**  create a rectified contour
 * @param distanceTolerance no longer used
 * @param lengthScale the number of points per unit of length in [RectifiedPath.points], default value is 1.0
 * @param lengthTolerance the largest error of the lengths along the path, in its units, default value is 1E-3
 */
fun Segment2D.rectified(
    distanceTolerance: Double = 0.5,
    lengthScale: Double = 1.0,
    lengthTolerance: Double = 1E-3
): RectifiedContour {
    return RectifiedContour(this.contour, distanceTolerance, lengthScale, lengthTolerance)
}