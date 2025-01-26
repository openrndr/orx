package blend

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.shapes.blend.blend
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.PI
import kotlin.math.cos

/**
 * Demonstration of non-uniform contour blending
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val a = Circle(Vector2.ZERO, 90.0).contour
        val b = regularStar(5, 30.0, 90.0, center = Vector2.ZERO, phase = 180.0)
        val blend = a.blend(b)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fill = ColorRGBa.BLACK
            drawer.bounds.grid(3, 3).flatten().forEachIndexed { index, it ->
                drawer.isolated {
                    drawer.translate(it.center)
                    drawer.contour(blend.mix { t -> cos(t * PI * 2.0 + index * PI * 2.0 / 9.0 + seconds) * 0.5 + 0.5 })
                }
            }
        }
    }
}
