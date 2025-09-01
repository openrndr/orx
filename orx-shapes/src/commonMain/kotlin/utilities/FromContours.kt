package org.openrndr.extra.shapes.utilities

import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour

/**
 * Creates a new `ShapeContour` by combining multiple `ShapeContour` instances.
 *
 * @param contours a list of `ShapeContour` to be combined; empty contours are removed
 * @param closed a boolean indicating whether the resulting `ShapeContour` should be closed
 * @param connectEpsilon the tolerance for connecting contours, default is 1E-6
 * @return a new `ShapeContour` combining the input contours
 */
fun ShapeContour.Companion.fromContours(contours: List<ShapeContour>, closed: Boolean, connectEpsilon:Double=1E-6) : ShapeContour {
    @Suppress("NAME_SHADOWING") val contours = contours.filter { !it.empty }
    if (contours.isEmpty()) {
        return EMPTY
    }
    return contour {
        moveTo(contours.first().position(0.0))
        for (c in contours.windowed(2,1,true)) {
            copy(c[0])
            if (c.size == 2) {
                val d = c[0].position(1.0).distanceTo(c[1].position(0.0))
                if (d > connectEpsilon ) {
                    lineTo(c[1].position(0.0))
                }
            }
        }
        if (closed) {
            close()
        }
    }
}