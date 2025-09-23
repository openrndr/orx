import org.openrndr.application
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.colormap.GrayscaleColormap
import kotlin.math.sin

/**
 * The [GrayscaleColormap] uses the red channel of a colorBuffer
 * to produce a gray scale image. The `curve` parameter is used as
 * an exponent to bias the result up or down. 1.0 produces a linear
 * transformation.
 */
fun main() = application {
    program {
        val colormap = GrayscaleColormap()
        val image = loadImage("demo-data/images/image-001.png")
        val colormapImage = image.createEquivalent(type = ColorType.FLOAT32)
        extend {
            colormap.curve = 1.0 + sin(seconds) * .5
            colormap.apply(image, colormapImage)
            drawer.image(colormapImage)
        }
    }
}
