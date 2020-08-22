// Draw rectangles shaded in RGB and HSLUV space

import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extras.color.spaces.ColorHSLUVa
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() {
    application {
        program {
            // -- this block is for automation purposes only
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
            extend {
                drawer.stroke = null
                for (a in 0 until 360 step 12) {
                    val pos = Vector2(0.0, 110.0)
                    drawer.isolated {
                        translate(bounds.center)
                        rotate(a * 1.0)

                        fill = ColorHSLUVa(a * 1.0, 1.0, 0.5).toRGBa().toSRGB()
                        rectangle(Rectangle(pos * 1.2, 40.0, 300.0))

                        fill = ColorHSLa(a * 1.0, 1.0, 0.5).toRGBa()
                        rectangle(Rectangle.fromCenter(pos, 30.0, 60.0))
                    }
                }
                drawer.fontMap = font
                drawer.fill = ColorRGBa.WHITE
                drawer.text("HSLa", width * 0.48, height * 0.73)
                drawer.text("HSLUVa", width * 0.8, height * 0.52)
                drawer.text("hue: 0 to 360, saturation: 1.0, " +
                        "lightness: 0.5", 50.0, 460.0)
            }
        }
    }
}