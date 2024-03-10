import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.createEquivalent
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.patterns.Checkers

fun main() {
    application {
        program {
            val fd = FluidDistort()
            val checkers =  Checkers()

            val image = colorBuffer(width, height)
            val distorted = image.createEquivalent()
            checkers.size = 64.0
            checkers.apply(emptyArray(), image)

            extend {
                fd.blend = mouse.position.x/width
                fd.apply(image, distorted)
                drawer.image(distorted)
            }
        }
    }
}