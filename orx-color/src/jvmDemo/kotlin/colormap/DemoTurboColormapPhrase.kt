package colormap

import org.openrndr.application
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.shaderphrases.preprocess

fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        ColormapPhraseBook.register()
        val style = shadeStyle {
            fragmentPreamble = "#pragma import colormap.turbo_colormap".preprocess()
            fragmentTransform = "x_fill.rgb = turbo_colormap(c_boundsPosition.x);"
        }
        extend {
            drawer.shadeStyle = style
            drawer.rectangle(drawer.bounds)
        }
    }
}
