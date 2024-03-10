import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.dither.LumaHalftone

fun main() {
    application {
        program {
            val image = loadImage("demo-data/images/image-001.png")
            val filteredImage = image.createEquivalent()
            val lumaHalftone = LumaHalftone()
            extend {
                lumaHalftone.rotation = -15.0
                lumaHalftone.freq0 = 100.0
                lumaHalftone.gain1 = 1.0
                lumaHalftone.threshold = 0.5
                lumaHalftone.phase0 = seconds*0.1
                lumaHalftone.phase1 = -seconds*0.1
                lumaHalftone.apply(image, filteredImage)
                lumaHalftone.invert = seconds.mod(2.0) < 1.0
                drawer.image(filteredImage)
            }
        }
    }
}