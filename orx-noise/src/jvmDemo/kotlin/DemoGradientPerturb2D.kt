import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.noise.gradientPerturbFractal
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import kotlin.math.absoluteValue

fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val cb = colorBuffer(width, height)
        val shad = cb.shadow
        extend {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val p =
                        gradientPerturbFractal(300, frequency = 0.8, position = Vector2(seconds + x / 320.0, y / 240.0))
                    val d = simplex(300, p.x, p.y + seconds, seconds).absoluteValue
                    shad[x, y] = ColorRGBa(d, d, d, 1.0)
                }
            }
            shad.upload()
            drawer.image(cb)
        }
    }
}