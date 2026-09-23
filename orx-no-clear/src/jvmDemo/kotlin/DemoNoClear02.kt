import org.openrndr.application
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.BlendMode
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorType
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noclear.NoClear
import org.openrndr.math.Polar
import kotlin.math.sin

/**
 * Demonstrates the `NoClear` extension with `FLOAT32` color type.
 *
 * The program draws circles around the center using additive blending.
 * The fill colors are dark and accumulate to create a visible effect.
 *
 * By default, the color type is `UINT8`, which provides 256 brightness levels
 * per color channel (red, green, blue).
 *
 * With `UINT8`, very small color increments (like a HSV value below 1.0/256.0,
 * e.g., 0.0035 instead of 0.035) would be too small to register, and the brightness
 * would not increase. Using `FLOAT32` solves this by supporting a finer precision.
 */
fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        if (System.getProperty("takeScreenshot") == "true") {
            extensions.filterIsInstance<SingleScreenshot>().forEach {
                it.delayFrames = 200
            }
        }

        extend(NoClear()) {
            colorType = ColorType.FLOAT32
            backdrop = {
                // Starting design: a dark gray background with a black ring
                drawer.clear(rgb(0.01))
                drawer.fill = null
                drawer.strokeWeight = 24.0
                drawer.stroke = ColorRGBa.BLACK
                drawer.circle(drawer.bounds.center, 180.0)
            }
        }

        extend {
            val theta = frameCount * 17.0
            val r = sin(frameCount * 0.6) * 100.0 + 100.0
            drawer.drawStyle.blendMode = BlendMode.ADD
            drawer.stroke = null
            drawer.fill = ColorHSVa(theta * 4.0, 0.8, 0.035).toRGBa()
            drawer.circle(drawer.bounds.center + Polar(theta, r * 0.9).cartesian, r)
        }
    }
}
