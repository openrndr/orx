import org.openrndr.application
import org.openrndr.boofcv.binding.ImageFlowProcessor
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to calculate the movement of pixels using an image flow algorithm.
 * Not ideal for real-time purposes.
 */
fun main() = application {
    program {
        val rt1 = renderTarget(width, height) { colorBuffer() }
        val rt2 = renderTarget(width, height) { colorBuffer() }
        val cb = colorBuffer(width, height, format = ColorFormat.RG, type = ColorType.FLOAT32)
        val flow = ImageFlowProcessor(width, height)
        extend {
            rt1.colorBuffer(0).copyTo(rt2.colorBuffer(0))
            drawer.isolatedWithTarget(rt1) {
                clear(ColorRGBa.PINK)
                translate(bounds.center)
                rotate(seconds * 5.0)
                rectangle(Rectangle.fromCenter(Vector2.ZERO, 400.0, 200.0))
            }
            flow.process(rt1.colorBuffer(0), rt2.colorBuffer(0), cb)
            drawer.image(cb)
        }
    }
}