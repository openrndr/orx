package metro

import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.metro.metro
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.random.Random

/**
 * Metro map style schematization of shapes and open paths. From left to right the number of allowed directions is
 * 2 (rectilinear), 3 (hexilinear), 4 (octilinear) and 6. The top row shows a closed shape, the middle row a noisy
 * open path, the bottom row the same path simplified further using a segment penalty and the bounds cost.
 * Gray: the input, colored: the schematized contours.
 */
fun main() {
    application {
        configure {
            width = 1000
            height = 720
        }
        program {
            val random = Random(0)
            val directions = listOf(2, 3, 4, 6)
            val cellWidth = width / directions.size.toDouble()

            val blob = ShapeContour.fromPoints(List(9) { i ->
                Polar(i * 360.0 / 9.0, if (i % 2 == 0) 100.0 else 60.0).cartesian
            }, closed = true).hobbyCurve()
            val shape = Shape(listOf(blob, Circle(Vector2(10.0, 5.0), 30.0).contour.reversed))

            var position = Vector2(-100.0, 0.0)
            var heading = 0.0
            val path = List(40) {
                heading = heading * 0.8 + Double.uniform(-40.0, 40.0, random)
                position += Polar(heading, 5.0).cartesian + Vector2.uniform(-2.0, 2.0, random)
                position
            }.let { ShapeContour.fromPoints(it, closed = false) }

            extend {
                drawer.clear(ColorRGBa.WHITE)
                drawer.fill = null
                for ((i, d) in directions.withIndex()) {
                    val cell = Rectangle(i * cellWidth, 0.0, cellWidth, height.toDouble())
                    drawer.isolated {
                        drawer.translate(cell.center.x, height * 0.28)
                        drawer.stroke = ColorRGBa.GRAY
                        drawer.strokeWeight = 1.0
                        drawer.shape(shape)
                        drawer.stroke = ColorRGBa.fromHex(0xe6194b)
                        drawer.strokeWeight = 3.0
                        drawer.shape(shape.metro(directions = d))
                    }
                    drawer.isolated {
                        drawer.translate(cell.center.x, height * 0.62)
                        drawer.stroke = ColorRGBa.GRAY
                        drawer.strokeWeight = 1.0
                        drawer.contour(path)
                        drawer.fill = ColorRGBa.GRAY
                        drawer.circles(path.segments.map { it.start }, 2.0)
                        drawer.fill = null
                        drawer.stroke = ColorRGBa.fromHex(0x4363d8)
                        drawer.strokeWeight = 3.0
                        drawer.contour(path.metro(directions = d))
                        drawer.translate(0.0, 100.0)
                        drawer.stroke = ColorRGBa.GRAY
                        drawer.strokeWeight = 1.0
                        drawer.contour(path)
                        drawer.stroke = ColorRGBa.fromHex(0xf58231)
                        drawer.strokeWeight = 3.0
                        drawer.contour(path.metro(directions = d, segmentPenalty = 100.0, boundsWeight = 1.0))
                    }
                }
            }
        }
    }
}
