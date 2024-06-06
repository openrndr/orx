import org.openrndr.application
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.shaderphrases.preprocess

fun main() = application {
    program {
        ColormapPhraseBook.register()
        val style = shadeStyle {
            fragmentPreamble = "#pragma import colormap.turbo_colormap".preprocess()
            fragmentTransform = "x_fill.rgb = turbo_colormap(c_boundsPosition.x);"
        }
        extend {
            drawer.shadeStyle = style
            drawer.rectangle(0.0, 0.0, width.toDouble(), height.toDouble())
        }
    }
}
