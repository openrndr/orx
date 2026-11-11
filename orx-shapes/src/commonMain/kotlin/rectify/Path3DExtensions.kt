package org.openrndr.extra.shapes.rectify

import org.openrndr.shape.Path3D

/** create a rectified contour
 * @param distanceTolerance no longer used
 * @param lengthScale the number of points per unit of length in [RectifiedPath.points], default value is 1.0
 * @param lengthTolerance the largest error of the lengths along the path, in its units, default value is 1E-3
 **/
fun Path3D.rectified(
    distanceTolerance: Double = 0.5,
    lengthScale: Double = 1.0,
    lengthTolerance: Double = 1E-3
): RectifiedPath3D =
    RectifiedPath3D(this, distanceTolerance, lengthScale, lengthTolerance)