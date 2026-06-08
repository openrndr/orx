package org.openrndr.extra.shapes.polygon

import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import org.openrndr.shape.contours

fun ShapeContour.toPolygon(): Polygon2D {
    require(closed) { "contour must be closed" }
    return Polygon2D(segments.map { it.start })
}

fun Shape.toComplexPolygon(): ComplexPolygon2D {
    require(contours.all { it.closed }) { "all contours must be closed" }
    return ComplexPolygon2D(contours.first().toPolygon(), contours.drop(1).map { it.toPolygon() })
}

val Polygon2D.contour: ShapeContour
    get() {
        return ShapeContour.fromPoints(points, true)
    }

val ComplexPolygon2D.shape: Shape
    get() {
        return Shape(listOf(this.outer.contour) + this.holes.map { it.contour } )
    }