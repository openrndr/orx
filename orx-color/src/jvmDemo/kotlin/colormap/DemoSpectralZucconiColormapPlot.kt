package colormap

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.color.colormaps.spectralZucconi6
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.math.Vector2

/**
 * This demo uses the shader based `spectral_zucconi6()` function to fill the background,
 * then visualizes the red, green and blue components of the colors used in the background
 * as red, green and blue line strips.
 *
 * The Vector2 points for the line strips are calculated only once when the program starts.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        ColormapPhraseBook.register()
        val backgroundStyle = shadeStyle {
            fragmentPreamble = "#pragma import colormap.spectral_zucconi6".preprocess()
            fragmentTransform = "x_fill.rgb = spectral_zucconi6(c_boundsPosition.x);"
        }

        // Function that expects as an argument a function to convert a ColorRGBa into a Double,
        // and returns a list of Vector2 coordinates.
        fun getColormapPoints(
            block: ColorRGBa.() -> Double
        ) = List(width) { x ->
            Vector2(
                x.toDouble(),
                (1.0 - block(spectralZucconi6(x / width.toDouble()))) * height
            )
        }

        val redPoints = getColormapPoints { r }
        val greenPoints = getColormapPoints { g }
        val bluePoints = getColormapPoints { b }
        extend {
            drawer.run {
                shadeStyle = backgroundStyle
                rectangle(bounds)
                shadeStyle = null

                stroke = ColorRGBa.RED
                lineStrip(redPoints)

                stroke = ColorRGBa.GREEN
                lineStrip(greenPoints)

                stroke = ColorRGBa.BLUE
                lineStrip(bluePoints)
            }
        }
    }
}
