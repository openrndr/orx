package org.openrndr.extra.shapes.functions

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

val ShapeContour.positionFunction: (t: Double) -> Vector2
    get() = { t: Double -> position(t) }

val RectifiedContour.positionFunction: (t: Double) -> Vector2
    get() = { t: Double -> position(t) }
