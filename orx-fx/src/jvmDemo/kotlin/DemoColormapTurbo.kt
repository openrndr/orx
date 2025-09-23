import org.openrndr.application
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.colormap.TurboColormap
import kotlin.math.sin

/**
 * Demonstrates the use of the [TurboColormap] effect, which
 * maps values of the RED color channel to Turbo Colormap according to
 * [Turbo, An Improved Rainbow Colormap for Visualization](https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html)
 * by Google.
 */
fun main() = application {
    program {
        val colormap = TurboColormap()
        val image = loadImage("demo-data/images/image-001.png")
        val colormapImage = image.createEquivalent(type = ColorType.FLOAT32)
        extend {
            colormap.curve = 1.0 + sin(seconds) * .5
            colormap.apply(image, colormapImage)
            drawer.image(colormapImage)
        }
    }
}
