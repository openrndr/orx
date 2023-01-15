import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.DirectionalBlur
import org.openrndr.math.smoothstep
import kotlin.math.cos
import kotlin.math.sin

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
            db.apply(arrayOf(rt.colorBuffer(0), direction), blurred)
            drawer.image(blurred)
        }
    }
}