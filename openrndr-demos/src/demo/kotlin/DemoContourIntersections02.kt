import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.intersections

fun main() = application {
    program {
        extend {
            val circle = Circle(mouse.position, 200.0).contour

            drawer.fill = null
            for (y in 50 until height step 100) {
                for (x in 50 until width step 100) {

                    for (ring in 0 until 10) {
                        val r = Rectangle.fromCenter(
                            Vector2(x * 1.0, y * 1.0),
                            90.0 - ring * 8.0,
                            90.0 - ring * 8.0
                        ).contour

                        val ints = intersections(circle, r)
                        if (ints.isEmpty()) {
                            drawer.stroke = ColorRGBa.GREEN
                            drawer.contour(r)
                        } else {
                            drawer.stroke = ColorRGBa.WHITE
                            ints.map { it.b.contourT }.let { it + it.take(1) }.zipWithNext().forEach {
                                val end = if (it.second <= it.first) it.second + 1.0 else it.second
                                val sub = r.sub(it.first, end)
                                val length = sub.length
                                val ta = sub.tForLength(2.0)
                                val tb = sub.tForLength(length - 2.0)
                                drawer.contour(sub.sub(ta, tb))
                            }
                        }
                    }
                }
            }
        }
    }
}
