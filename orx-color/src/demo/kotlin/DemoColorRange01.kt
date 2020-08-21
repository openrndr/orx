// Create a simple rectangle composition based on colors sampled from image

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.color.palettes.rangeTo
import org.openrndr.extras.color.presets.CORAL
import org.openrndr.extras.color.spaces.toHSLUVa

fun main() = application {
    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend {

            drawer.isolated {
                for (c in ColorRGBa.PINK..ColorRGBa.BLUE.toHSVa() blend 10) {
                    drawer.fill = c
                    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
                    drawer.translate(0.0, 40.0)
                }
            }
            drawer.translate(50.0, 0.0)
            drawer.isolated {
                for (c in ColorRGBa.PINK..ColorRGBa.BLUE blend 10) {
                    drawer.fill = c
                    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
                    drawer.translate(0.0, 40.0)
                }
            }
            drawer.translate(50.0, 0.0)

            drawer.isolated {
                for (c in ColorRGBa.PINK..ColorRGBa.BLUE.toHSLUVa() blend 10) {
                    drawer.fill = c.toSRGB()
                    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
                    drawer.translate(0.0, 40.0)
                }
            }

            drawer.translate(50.0, 0.0)

            drawer.isolated {
                for (c in ColorRGBa.PINK..ColorRGBa.BLUE.toXSVa() blend 10) {
                    drawer.fill = c.toSRGB()
                    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
                    drawer.translate(0.0, 40.0)
                }
            }

            drawer.translate(50.0, 0.0)

            drawer.isolated {
                for (c in ColorRGBa.PINK..ColorRGBa.BLUE.toLUVa() blend 10) {
                    drawer.fill = c.toSRGB()
                    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
                    drawer.translate(0.0, 40.0)
                }
            }

            drawer.translate(50.0, 0.0)

            drawer.isolated {
                for (c in ColorRGBa.PINK..ColorRGBa.BLUE.toLCHUVa() blend 10) {
                    drawer.fill = c.toSRGB()
                    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
                    drawer.translate(0.0, 40.0)
                }
            }
        }
    }
}