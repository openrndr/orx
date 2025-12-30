import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.toHSLUVa
import org.openrndr.math.Polar
import kotlin.math.sqrt

/**
 * Visualizes the HSLUV color space by drawing a phyllotaxis pattern.
 *
 * The program also demonstrates how to create a function that returns a `sequence`.
 * Unlike collections, sequences don't contain elements, they produce them while iterating.
 * https://kotlinlang.org/docs/sequences.html
 *
 * Each position in the phyllotaxis is rendered as a spherical gradient by repeatedly drawing
 * each circle with different sizes and a fill color.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    val g = Math.PI * 2.0 * (1.0 - 1.0 / 1.61803398875)

    fun phyllotaxis(count: Int) = sequence {
        for (i in 0 until count) {
            yield(Polar(Math.toDegrees(i * 1.0), g * i))
        }
    }

    program {
        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.stroke = null
            drawer.strokeWeight = 0.0

            val color = ColorRGBa.RED
            val hc = color.toHSLUVa()

            val count = 400
            val bobRadius = 20.0

            for (polar in phyllotaxis(count)) {
                val h = polar.theta
                val s = polar.radius / (count * g)
                val position = polar.cartesian / (count * g) * (width / 2.0 - bobRadius) + drawer.bounds.center
                for (l in 9 downTo 1) {
                    drawer.fill = hc.shiftHue(h).saturate(s).shade((9 - l) / 4.5).toRGBa().toSRGB()
                    drawer.circle(position, sqrt(s) * 20.0 * l / 9.0)
                }
            }
        }
    }
}
