import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.*

fun main() {
    application {
        program {
            extend {
                drawer.points {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val c = cubicHermite3D(100, (x + y) * 0.04, (x - y) * 0.04, seconds * 1.0) * 0.5 + 0.5
                            fill = ColorRGBa(c, c, c, 1.0)
                            point(x.toDouble(), y.toDouble())
                        }
                    }
                }
            }
        }
    }
}