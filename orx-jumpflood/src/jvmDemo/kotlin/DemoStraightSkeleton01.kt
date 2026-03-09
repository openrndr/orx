import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.jumpfill.fx.StraightSkeleton
import org.openrndr.extra.noise.simplex

/**
 * Demonstrates the use of the [StraightSkeleton] filter.
 *
 * The program draws animated circles and a mouse-controlled ring onto a `RenderTarget`,
 * then applies the `StraightSkeleton` filter to the result, writing the result into a `ColorBuffer`.
 *
 * The `StraightSkeleton` filter generates a texture highlighting the "spine" of shapes:
 * circles as a dot in their center, rectangles as lines, rings as circles, and more complex shapes
 * as branching lines.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val straightSkeleton = StraightSkeleton()

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
            straightSkeleton.apply(input.colorBuffer(0), field)
            drawer.image(field)
        }
    }
}
