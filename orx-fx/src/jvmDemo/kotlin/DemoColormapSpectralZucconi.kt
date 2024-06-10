import org.openrndr.application
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.colormap.SpectralZucconiColormap
import kotlin.math.sin

fun main() {
    application {
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
}