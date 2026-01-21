package org.openrndr.extra.shapes.functions

import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.math.Matrix44

/**
 *
 */
val PosePath3D.poseFunction: (t: Double) -> Matrix44
    get() = { t -> pose(t) }
