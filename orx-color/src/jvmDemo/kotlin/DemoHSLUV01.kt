// Draw rectangles shaded in RGB and HSLUV space

import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.spaces.ColorHSLUVa
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 26.0)
        extend {
            drawer.stroke = null
            drawer.clear(rgb(0.3))
            val s = mouse.position.x / width
            val l = mouse.position.y / height
            for (a in 0 until 360 step 12) {
                val pos = Vector2(0.0, 110.0)
                drawer.isolated {
                    translate(bounds.center)
                    rotate(a * 1.0)

                    fill = ColorHSLUVa(a * 1.0, s, l).toRGBa().toSRGB()
                    rectangle(Rectangle(pos * 1.2, 40.0, 300.0))

                    fill = ColorHSLa(a * 1.0, s, l).toRGBa()
                    rectangle(Rectangle.fromCenter(pos, 30.0, 60.0))
                }
            }
            drawer.fontMap = font
            drawer.fill = if (l > 0.8) ColorRGBa.BLACK else ColorRGBa.WHITE
            drawer.text("HSLa", width * 0.48, height * 0.73)
            drawer.text("HSLUVa", width * 0.8, height * 0.52)
            drawer.text(
                "hue: 0 to 360, " +
                        "saturation: ${String.format("%.02f", s)}, " +
                        "lightness: ${String.format("%.02f", l)}",
                30.0, height - 30.0
            )
        }
    }
}
