package org.openrndr.extra.shapes.utilities

import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour

/**
 * Create a contour from a list of contours
 */
fun ShapeContour.Companion.fromContours(contours: List<ShapeContour>, closed: Boolean, connectEpsilon:Double=1E-6) : ShapeContour {
    val contours = contours.filter { !it.empty }
    if (contours.isEmpty()) {
        return EMPTY
    }
    return contour {
        moveTo(contours.first().position(0.0))
        for (c in contours.windowed(2,1,true)) {
            copy(c[0])
            if (c.size == 2) {
                if (c[0].position(1.0).distanceTo(c[1].position(0.0)) > connectEpsilon ) {
                    lineTo(c[1].position(0.0))
                }
            }
        }
        if (closed) {
            close()
        }
    }
}