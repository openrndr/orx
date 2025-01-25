import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.palettes.analogous
import org.openrndr.extra.color.palettes.splitComplementary
import org.openrndr.extra.color.palettes.tetradic
import org.openrndr.extra.color.palettes.triadic
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.color.spaces.*

/**
 * Demonstrates the creation of color palettes using various available methods
 */
fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        // HueShiftableColor:
        // HPLuv HSL HSV LCHab LCHuv XSL XSV XSLuv HSLuv OKHSL OKHSV OKLCH
        val palette0 = RGB.PINK.analogous<HSLuv>(360.0, 10)
        val palette1 = RGB.RED.analogous<HSL>(240.0, 3)
        val palette2 = RGB.YELLOW.triadic<OKHSV>()
        val palette3 = RGB.CYAN.tetradic<OKLCH>()
        val palette4 = RGB.CYAN.tetradic<OKLCH>(0.5)
        val palette5 = RGB.ORANGE.splitComplementary<HPLuv>(0.2, true)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.stroke = ColorRGBa.BLACK.opacify(0.25)

            palette0.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.circle(100.0 + i * 50.0, 80.0, 50.0)
            }

            palette1.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.circle(100.0 + i * 50.0, 200.0, 50.0)
            }

            palette2.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.circle(440.0 + i * 50.0, 200.0, 50.0)
            }

            palette3.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.circle(100.0 + i * 50.0, 320.0, 50.0)
            }

            palette4.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.circle(400.0 + i * 50.0, 320.0, 50.0)
            }

            palette5.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.circle(100.0 + i * 50.0, 440.0, 50.0)
            }
        }
    }
}