package polygon

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.polygon.intersects
import org.openrndr.extra.shapes.polygon.toPolygon
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.Rectangle

fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.PINK)

                val p0 = Rectangle.fromCenter(drawer.bounds.center, 100.0, 100.0).contour.toPolygon()
                //val p1 = Rectangle.fromCenter(mouse.position, 50.0, 50.0).contour.toPolygon()

                val p1 = regularStar(75, 10.0, 30.0, mouse.position).contour.toPolygon()

                val start = System.currentTimeMillis()
                for (i in 0 until 10000) {
                    if (p0.intersects(p1)) {
                        drawer.stroke = ColorRGBa.RED
                    }
                }
                val end = System.currentTimeMillis()
                println("intersectsSweep took ${end - start} ms")

                drawer.lineLoop(p0)
                drawer.lineLoop(p1)


            }
        }
    }
}