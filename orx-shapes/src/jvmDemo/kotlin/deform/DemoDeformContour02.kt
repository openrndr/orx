package deform

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.shapes.deform.deform
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * An interactive demo of
 * [ShapeContour.deform](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.deform/deform.html)
 * that presents a circular contour and deforms it in real time based on the mouse position.
 *
 * By clicking the mouse, the temporary deformation becomes permanent, letting you sculpt a 2D shape
 * by deforming it repeatedly.
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
                listOf(Vector2(x=547.7, y=143.7), Vector2(x=442.2, y=259.8), Vector2(x=410.7, y=342.2)).forEach {
                    c = c.deform(it, 1.0, 10.0, 100.0)
                }
            }

            mouse.buttonDown.listen {
                c = c.deform(it.position, 1.0, 10.0, 100.0)
            }

            extend {
                val nc = c.deform(mouse.position, 1.0, 10.0, 100.0)

                drawer.stroke = ColorRGBa.PINK
                drawer.drawStyle.lineJoin = LineJoin.ROUND
                drawer.fill = null
                drawer.contour(nc)
                drawer.circles(nc.segments.map { it.start }, 4.0)
                drawer.circle(mouse.position, 10.0)
            }
        }
    }
}