import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.jumpfill.fx.Skeleton
import org.openrndr.extra.noise.simplex

/**
 * Demonstrates the use of the [Skeleton] filter.
 *
 * The program draws animated circles and a mouse-controlled ring onto a `RenderTarget`,
 * then applies the `Skeleton` filter to the result, writing the result into a `ColorBuffer`.
 *
 * The `Skeleton` filter generates a texture in which inner parts of shapes are connected to
 * the edges, creating a skeleton of the shape.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        val skeleton = Skeleton()

        val input = renderTarget(width, height) {
            colorBuffer()
        }
        val field = input.colorBuffer(0).createEquivalent(type = ColorType.FLOAT32)
        extend {
            drawer.isolatedWithTarget(input) {
                // -- draw something interesting
                drawer.stroke = null
                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = ColorRGBa.WHITE
                drawer.circle(mouse.position, 300.0)
                drawer.fill = ColorRGBa.BLACK
                drawer.circle(mouse.position, 150.0)
                drawer.fill = ColorRGBa.WHITE
                for (i in 0 until 30) {
                    val time = seconds * 0.25
                    val x = simplex(i * 20, time) * width / 2 + width / 2
                    val y = simplex(i * 20 + 5, time) * height / 2 + height / 2
                    val r = simplex(i * 30, time) * 50.0 + 50.0
                    drawer.circle(x, y, r)
                }
            }
            skeleton.apply(input.colorBuffer(0), field)
            drawer.image(field)
        }
    }
}
