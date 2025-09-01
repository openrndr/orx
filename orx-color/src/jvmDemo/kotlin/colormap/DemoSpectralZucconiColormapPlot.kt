package colormap

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.color.colormaps.spectralZucconi6
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.math.Vector2

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

        fun getColormapPoints(
            block: ColorRGBa.() -> Double
        ) = List(width) { x ->
            Vector2(
                x.toDouble(),
                height.toDouble()
                        - block(spectralZucconi6(x / width.toDouble()))
                        * height.toDouble()
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
                strokeWeight = 1.0
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
