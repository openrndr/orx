package colormap

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.color.colormaps.turboColormap
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.math.Vector2

/**
 * This demo uses the shader based `turbo_colormap()` function to fill the background,
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
            fragmentPreamble = "#pragma import colormap.turbo_colormap".preprocess()
            fragmentTransform = "x_fill.rgb = turbo_colormap(c_boundsPosition.x);"
        }
        fun getColormapPoints(
            block: ColorRGBa.() -> Double
        ) = List(width) { x ->
            Vector2(
                x.toDouble(),
                (1.0 - block(turboColormap(x / width.toDouble()))) * height
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
