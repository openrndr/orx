// Visualize HSLUV color space

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.color.spaces.toHSLUVa
import org.openrndr.math.Polar
import org.openrndr.math.Vector2

fun main() {
    application {
        program {
            // -- this block is for automation purposes only
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            extend {
                val color = ColorRGBa.RED
                val hc = color.toHSLUVa()
                drawer.stroke = null
                drawer.strokeWeight = 0.0
                for (h in 0 until 360 step 10) {
                    for (s in 0 until 10) {
                        for (l in 9 downTo 0) {
                            val position = Polar(h.toDouble(), s * 25.0).cartesian + Vector2(width/ 2.0, height / 2.0)
                            drawer.fill = hc.shiftHue(h.toDouble()).saturate(s/9.0).shade((9-l)/4.5).toRGBa().toSRGB()
                            drawer.circle(position, kotlin.math.sqrt(s/10.0)*25.0 * l/9.0)
                        }
                    }
                }
            }
        }
    }
}