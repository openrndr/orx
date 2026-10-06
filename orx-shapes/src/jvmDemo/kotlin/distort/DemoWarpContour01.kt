package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.distort.warp
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to [warp](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/warp.html)
 * a contour relative to a `base` and `warp` contours.
 *
 * The warping operation works by finding the nearest position and normal on the `base` contour,
 * then applying that displacement relative to the `warp` contour.
 *
 * This interactive program:
 * - Uses mouse position to create a rectified circle
 * - Uses a horizontal rectified contour as the warping `base`
 * - Uses a centered rectified circle as the `warp` contour
 *
 * Think of the `base` and `warp` contours as source and destination: the circle follows your mouse
 * and transforms relative to both contours. Horizontal mouse movement moves the warped circle
 * along the `base` and `warp` contours, while vertical movement changes its distance to both contours.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val c = Circle(drawer.bounds.center, 200.0).contour.rectified()

            extend {
                //val r = Rectangle.fromCenter(mouse.position, 100.0, 100.0).contour.rectified()
                val r = Circle(mouse.position, 50.0).contour.rectified()
                val b = drawer.bounds.horizontal(0.5).contour.rectified()
                val w = r.warp(b, c)

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(b.contour)
                drawer.contour(c.contour)
                drawer.contour(w)
            }
        }
    }
}