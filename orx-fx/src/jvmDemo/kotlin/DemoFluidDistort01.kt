import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.createEquivalent
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.patterns.Checkers

/**
 * Demonstrates [FluidDistort], a fluid simulation real time effect.
 * All pixels are slowly displaced in a turbulent manner as if they were a gas or a liquid.
 */
fun main() = application {
    program {
        val fd = FluidDistort()
        val checkers = Checkers()

        val image = colorBuffer(width, height)
        val distorted = image.createEquivalent()
        checkers.size = 64.0
        checkers.apply(emptyArray(), image)

        if (System.getProperty("takeScreenshot") == "true") {
            extensions.filterIsInstance<SingleScreenshot>().forEach {
                it.delayFrames = 150
            }
        }
        extend {
            // Ensure >0.01 for a better screenshot
            fd.blend = (mouse.position.x / width).coerceAtLeast(0.01)
            fd.apply(image, distorted)
            drawer.image(distorted)
        }
    }
}
