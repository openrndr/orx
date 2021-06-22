// Visualize HSLUV color space by drawing a phyllotaxis pattern

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.color.spaces.toHSLUVa
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import kotlin.math.sqrt

suspend fun main() {
    application {
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
            // -- this block is for automation purposes only
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            extend {
                drawer.clear(ColorRGBa.GRAY)
                val color = ColorRGBa.RED
                val hc = color.toHSLUVa()
                drawer.stroke = null
                drawer.strokeWeight = 0.0

                val count = 400
                val bobRadius = 20.0

                for (i in phyllotaxis(count)) {
                    val h = i.theta
                    val s = i.radius / (count * g)
                    for (l in 9 downTo 0) {
                        val position = i.cartesian / (count * g) * (width / 2.0 - bobRadius) + Vector2(width / 2.0, height / 2.0)
                        drawer.fill = hc.shiftHue(h).saturate(s).shade((9 - l) / 4.5).toRGBa().toSRGB()
                        drawer.circle(position, sqrt(s) * 20.0 * l / 9.0)
                    }
                }
            }
        }
    }
}