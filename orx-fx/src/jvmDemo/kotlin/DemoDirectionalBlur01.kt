import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.DirectionalBlur
import org.openrndr.math.smoothstep
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates how to use [DirectionalBlur] by creating a `direction`
 * ColorBuffer in which the red and green components of the pixels point
 * in various directions where to sample pixels from. All the pixel colors
 * of the ColorBuffer are set one by one using two for loops.
 *
 * Note the FLOAT32 color type of the buffer to allow for negative values,
 * so sampling can happen from every direction.
 *
 * Every 60 animation frames the `centerWindow` property is toggled
 * between true and false to demonstrate how the result changes.
 *
 */
fun main() = application {
    program {
        val db = DirectionalBlur()
        val rt = renderTarget(width, height) {
            colorBuffer()
        }

        val blurred = colorBuffer(width, height)
        val direction = colorBuffer(width, height, type = ColorType.FLOAT32)
        val s = direction.shadow
        for (y in 0 until height) {
            for (x in 0 until width) {
                val a = smoothstep(0.45, 0.55, cos((x + y) * 0.01) * 0.5 + 0.5)
                s[x, y] = ColorRGBa(cos(y * .1) * a, sin(x * 0.1) * a, 0.0, 1.0)
            }
        }

        s.upload()
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.BLACK)
                drawer.image(image)
            }
            db.window = 10
            db.centerWindow = frameCount % 120 > 60
            db.apply(arrayOf(rt.colorBuffer(0), direction), blurred)
            drawer.image(blurred)
        }
    }
}