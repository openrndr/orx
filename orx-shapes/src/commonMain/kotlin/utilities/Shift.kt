package org.openrndr.extra.shapes.utilities

import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.shape.ShapeContour

/**
 * Shifts the starting point of the current `ShapeContour` by a specified normalized distance `dt`.
 *
 * @param dt the normalized distance by which to shift the starting point. The value is interpreted
 * as a fraction of the total contour length (from 0.0 to 1.0). Positive values shift forward along
 * the contour, while negative values shift backward.
 * @return a new `ShapeContour` with the starting point shifted by the specified distance.
 * If the contour is not closed, the original contour is returned unaltered.
 */
fun ShapeContour.shift(dt: Double): ShapeContour {
    if (!closed) return this

    val t = dt.mod(1.0)
    val parts = splitAt(listOf(t))
    return ShapeContour.fromContours(parts.reversed(), closed)
}

/**
 * Shifts the contour by a specified amount along its parameterized path.
 * If the contour is not closed, it returns the original contour unaltered.
 *
 * @param dt the shift amount, where 1.0 represents the entire length of the contour
 *           and positive values move forward while negative values move backward.
 * @return a new shifted `ShapeContour` if the contour is closed, or the original contour otherwise.
 */
fun RectifiedContour.shift(dt: Double): ShapeContour {
    if (!contour.closed) return contour

    val t = dt.mod(1.0)
    val parts = splitAt(listOf(t))
    return ShapeContour.fromContours(parts.reversed(), contour.closed)
}