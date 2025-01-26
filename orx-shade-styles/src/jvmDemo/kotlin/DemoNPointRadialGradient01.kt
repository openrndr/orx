import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extra.shadestyles.NPointRadialGradient
import org.openrndr.shape.Circle
import kotlin.random.Random

/**
 * Demonstrate using a multicolor radial gradient.
 * The gradient has 5 colors (first and last ones are transparent).
 * Any of the properties can be animated, including colors and points.
 * See DemoNPointLinearGradient01.kt for an example of animated properties.
 */
fun main() = application {
    program {
        val gradient = NPointRadialGradient(
            arrayOf(
                ColorRGBa.PINK.opacify(0.0),
                ColorRGBa.PINK, ColorRGBa.WHITE, ColorRGBa.PINK,
                ColorRGBa.PINK.opacify(0.0)
            ), arrayOf(0.0, 0.4, 0.5, 0.6, 1.0)
        )

        val circles = List(25) {
            Circle(
                Random.nextDouble() * drawer.width,
                Random.nextDouble() * drawer.height,
                Random.nextDouble() * 150.0
            )
        }

        extend {
            drawer.run {
                clear(rgb(0.2))
                shadeStyle = gradient
                fill = ColorRGBa.WHITE
                stroke = null
                circles(circles)
            }
        }
    }
}
