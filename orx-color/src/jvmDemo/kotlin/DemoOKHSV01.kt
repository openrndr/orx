import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.spaces.ColorOKHSLa
import org.openrndr.extra.color.spaces.ColorOKHSVa

/**
 * Shows the color green shifted in hue over 360 degrees in 36 steps, side by side in 4 color spaces:
 * OKHSV, HSV, HSL and OKHSL.
 *
 * To shift hues the method `shiftHue()` is applied. The resulting colors are then converted
 * from each color space to RGB so they can be used for drawing.
 */
fun main() = application {
    configure {
        width = 720
        height = 160
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 18.0)

        extend {
            drawer.clear(rgb(0.2))
            drawer.translate(50.0, 40.0)
            drawer.stroke = ColorRGBa.BLACK.opacify(0.8)

            val c = ColorRGBa.GREEN

            val okhsv = ColorOKHSVa.fromColorRGBa(c)
            val hsv = c.toHSVa()
            val hsl = c.toHSLa()
            val okhsl = ColorOKHSLa.fromColorRGBa(c)

            val side = 16.0

            for (i in 0 until 36) {
                val shiftInDegrees = i * 10.0
                drawer.fill = okhsv.shiftHue(shiftInDegrees).toRGBa()
                drawer.rectangle(i * side, 0.0, side, side)

                drawer.fill = hsv.shiftHue(shiftInDegrees).toRGBa()
                drawer.rectangle(i * side, 20.0, side, side)

                drawer.fill = okhsl.shiftHue(shiftInDegrees).toRGBa()
                drawer.rectangle(i * side, 40.0, side, side)

                drawer.fill = hsl.shiftHue(shiftInDegrees).toRGBa()
                drawer.rectangle(i * side, 60.0, side, side)
            }

            // Write color space names
            drawer.fill = ColorRGBa.WHITE
            drawer.fontMap = font
            drawer.translate(36 * side + 5, 13.0)
            drawer.text("OKHSV", 0.0, 0.0)
            drawer.text("HSV", 0.0, 20.0)
            drawer.text("OKHSL", 0.0, 40.0)
            drawer.text("HSL", 0.0, 60.0)
        }
    }
}