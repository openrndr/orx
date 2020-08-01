import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Ellipse
import org.openrndr.shape.OrientedEllipse
import org.openrndr.shape.intersections

fun main() {
    application {
        program {
            val c1 = Ellipse(width / 2.0, height / 2.0, 200.0, 100.0).contour
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = null
                val c2 = OrientedEllipse(mouse.position, 100.0, 200.0, seconds*45.0).contour
                drawer.contour(c1)
                //drawer.contour(c2)
                val ints = intersections(c1, c2)

                if (ints.isEmpty()) {
                    drawer.contour(c2)
                } else {
                    (ints + ints.take(1)).map { it.contourTB }.zipWithNext().forEach {
                        val end = if (it.second <= it.first) it.second + 1.0 else it.second
                        val sub = c2.sub(it.first, end)
                        val l = sub.length
                        val ta = sub.tForLength(15.0)
                        val tb = sub.tForLength(l - 15.0)
                        drawer.contour(sub.sub(ta, tb))
                    }
                }
                for (i in ints) {
                    drawer.circle(i.position, 10.0)
                }
            }
        }
    }
}