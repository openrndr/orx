// Draw rectangles shaded in RGB and HSLUV space

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.color.spaces.toHSLUVa

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
                val color = ColorRGBa.PINK
                drawer.stroke = null
                for (i in 0 until 10) {
                    drawer.fill = color.shade(1.0 - i / 10.0)
                    drawer.rectangle(100.0, 100.0 + i * 20.0, 100.0, 20.0)

                    drawer.fill = color.toHSLUVa().shade(1.0 - i / 10.0).toRGBa().toSRGB()
                    drawer.rectangle(200.0, 100.0 + i * 20.0, 100.0, 20.0)
                }
            }
        }
    }
}