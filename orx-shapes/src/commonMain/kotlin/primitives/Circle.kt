package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.asRadians
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import org.openrndr.shape.Ellipse
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour as buildContour

/**
 * Convert [Circle] to [ShapeContour] with a number of segments equal to [segments]
 * @param segments the number of segments, at least 4
 * @param theta the starting angle of the circle, in degrees
 */
fun Circle.contour(segments: Int, theta: Double = 0.0): ShapeContour {
    return buildContour {
        val p = Polar(0.0, radius)
        moveTo(center + p.cartesian)
        for (i in 1 until segments + 1) {
            val lp = Polar(i * 360.0 / segments, radius).cartesian + center
            arcTo(radius, radius, 360.0 / segments, false, true, lp.x, lp.y)
        }
        close()
    }.transform(buildTransform {
        if (theta != 0.0) {
            translate(center)
            rotate(theta)
            translate(-center)
        }
    }
    )
}

/**
 * Convert [Ellipse] to [ShapeContour] with a number of segments equal to [segments]
 * @param segments the number of segments, at least 4
 * @param theta the starting angle of the ellipse, in degrees
 */
fun Ellipse.contour(segments: Int, theta: Double = 0.0): ShapeContour {
    return Circle(Vector2.ZERO, xRadius).contour(segments, theta).transform(
        buildTransform {
            translate(center)
            scale(1.0, yRadius / xRadius)
        }
    )
}