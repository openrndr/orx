package org.openrndr.extra.shapes.rectify

import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour

/** create a rectified contour
 * @param distanceTolerance distance tolerance to use, 0.5 is the default distance tolerance
 * @param lengthScale used to compute the size of the LUT, default value is 1.0
 **/
fun ShapeContour.rectified(distanceTolerance: Double = 0.5, lengthScale: Double = 1.0): RectifiedContour {
    return RectifiedContour(this, distanceTolerance, lengthScale)
}

/**  create a rectified contour
 *   @param distanceTolerance distance tolerance to use, 0.5 is the default distance tolerance
 *   @param lengthScale used to compute the size of the LUT, default value is 1.0
 *
 * */
fun Segment2D.rectified(distanceTolerance: Double = 0.5, lengthScale: Double = 1.0): RectifiedContour {
    return RectifiedContour(this.contour, distanceTolerance, lengthScale)
}