package org.openrndr.boofcv.binding

import boofcv.alg.filter.binary.Contour
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

fun Contour.toShape(closed: Boolean = false, getInternal: Boolean, getExternal: Boolean): Shape {
    val contours = mutableListOf<ShapeContour>()

    if (getExternal) {
        val externalPoints = external.toVector2s()
        contours.addAll(listOf(ShapeContour.fromPoints(externalPoints, closed)))
    }
    if (getInternal) {
        val internalCurves = internal.filter { it.size >= 3 }.map { it.toVector2s() }
        contours.addAll(internalCurves.map { internalCurve ->
            ShapeContour.fromPoints(internalCurve, closed)
        })
    }
    return Shape(contours)
}

fun List<Contour>.toShapes(closed: Boolean = false,
                           internal: Boolean = true,
                           external: Boolean = true): List<Shape> {
    return this.filter { it.external.size >= 3 }.map {
        it.toShape(closed, internal, external)
    }
}

fun List<Contour>.toShapeContours(closed: Boolean = false,
                           internal: Boolean = true,
                           external: Boolean = true): List<ShapeContour> {
    val contours = mutableListOf<ShapeContour>()
    this.forEach { contour ->
        if(contour.external.size >= 3) {
            if (external) {
                val externalPoints = contour.external.toVector2s()
                contours.add(ShapeContour.fromPoints(externalPoints, closed))
            }
            if (internal) {
                val internalCurves = contour.internal.filter { it.size >= 3 }
                        .map { it.toVector2s() }
                internalCurves.forEach { internalContour ->
                    contours.add(ShapeContour.fromPoints(internalContour, closed))
                }
            }
        }
    }
    return contours
}
