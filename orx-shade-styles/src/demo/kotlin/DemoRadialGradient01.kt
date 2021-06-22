import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot

import org.openrndr.extra.shadestyles.radialGradient
import kotlin.math.cos

suspend fun main() {
    application {
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend {
                drawer.shadeStyle = radialGradient(
                        ColorRGBa.PINK,
                        ColorRGBa.PINK.toHSVa().shiftHue(180.0).shade(0.5).toRGBa(),
                        exponent = cos(seconds)*0.5+0.5
                )
                drawer.rectangle(120.0, 40.0, 400.0, 400.0)
            }
        }
    }
}