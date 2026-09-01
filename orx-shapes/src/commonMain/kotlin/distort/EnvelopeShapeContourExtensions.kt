package org.openrndr.extra.shapes.distort

import org.openrndr.extra.shapes.bezierpatches.BezierPatch
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

/**
 * Distorts the current `ShapeContour` by mapping its points to a `target`
 * `BezierPatch` using the parameterization derived from the `basis` rectangle.
 *
 * @param basis the `Rectangle` that serves as the parameterization reference for the contour.
 * @param target the `BezierPatch` that provides the target mapping using its `u, v` parameter space.
 * @return a new `ShapeContour` transformed by the provided target `BezierPatch`.
 */
fun ShapeContour.envelope(basis: Rectangle, target: BezierPatch): ShapeContour {
    return distort { p ->
        val uv = (p - basis.corner) / basis.dimensions
        target.position(uv.x, uv.y)
    }
}

/**
 * Transforms the shape by distorting its contours based on a mapping between
 * a reference rectangle and a target Bézier patch.
 *
 * Each contour in the shape is processed by applying a mapping from the `basis`
 * rectangle's parameter space to the `target` Bézier patch, resulting in a new set
 * of contours that represent the transformed shape.
 *
 * @param basis the rectangle used to parameterize the original contours of the shape.
 * @param target the target Bézier patch that defines the destination space for the transformation.
 * @return a new shape whose contours are transformed according to the mapping defined by the `basis` and `target`.
 */
fun Shape.envelope(basis: Rectangle, target: BezierPatch): Shape {
    val contours = this.contours.map { it.envelope(basis, target) }
    return Shape(contours)
}