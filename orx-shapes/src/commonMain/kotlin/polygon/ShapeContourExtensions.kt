package org.openrndr.extra.shapes.polygon

import org.openrndr.shape.ShapeContour

fun ShapeContour.toPolygon(): Polygon2D {
    require(closed) { "contour must be closed" }
    return Polygon2D(segments.map { it.start })
}