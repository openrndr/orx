package org.openrndr.extra.shapes.rectify

import org.openrndr.shape.Path3D

/** create a rectified contour
 * @param distanceTolerance distance tolerance to use, 0.5 is the default distance tolerance
 * @param lengthScale used to compute the size of the LUT, default value is 1.0
 **/
fun Path3D.rectified(distanceTolerance: Double = 0.5, lengthScale: Double = 1.0): RectifiedPath3D =
    RectifiedPath3D(this, distanceTolerance, lengthScale)