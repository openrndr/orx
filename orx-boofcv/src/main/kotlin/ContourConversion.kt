package org.openrndr.boofcv.binding

import boofcv.alg.filter.binary.Contour
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

fun Contour.toShape(): Shape {
    val external = external.toVector2s()
    val internal = internal.filter { it.size > 0 }. map { it.toVector2s() }
    val contours = listOf(ShapeContour.fromPoints(external, false)) +
            internal.map {
                ShapeContour.fromPoints(it, false)
            }

    return Shape(contours)
}

fun List<Contour>.toShapes(): List<Shape> {
    return this.filter { it.external.isNotEmpty() }.map {
        it.toShape()
    }
}