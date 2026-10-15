package deform

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.deform.deform
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * [ShapeContour.deform](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.deform/deform.html)
 * deforms the current ShapeContour by applying an attraction force toward a given attractor point.
 * The deformation is influenced by various parameters including strength, falloff, and reference distance,
 * and adaptively refines the contour where necessary to meet an error tolerance.
 *
 * In this interactive demo, a circular shape is deformed based on the current mouse position.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            var c = Circle(drawer.bounds.center, 200.0).contour

            if (System.getProperty("takeScreenshot") == "true") {
                c = c.deform(Vector2(170.0), 1.0, 10.0, 100.0)
            }

            extend {
                val nc = c.deform(mouse.position, 1.0, 10.0, 100.0)

                drawer.stroke = ColorRGBa.PINK
                drawer.fill = null
                drawer.contour(nc)
            }
        }
    }
}