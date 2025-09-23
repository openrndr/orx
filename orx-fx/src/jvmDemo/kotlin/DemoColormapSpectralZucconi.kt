import org.openrndr.application
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.colormap.SpectralZucconiColormap
import kotlin.math.sin

/**
 * Demonstrates the [SpectralZucconiColormap], which
 * maps values of the RED color channel to the natural light dispersion
 * spectrum as described by Alan Zucconi in his
 * [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/)
 * article.
 */
fun main() = application {
    program {
        val colormap = SpectralZucconiColormap()
        val image = loadImage("demo-data/images/image-001.png")
        val colormapImage = image.createEquivalent(type = ColorType.FLOAT32)
        extend {
            colormap.curve = 1.0 + sin(seconds) * .5
            colormap.apply(image, colormapImage)
            drawer.image(colormapImage)
        }
    }
}
