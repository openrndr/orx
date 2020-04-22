import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noise.gradientPerturbFractal
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector3
import kotlin.math.absoluteValue

fun main() {
    application {
        program {
            val cb = colorBuffer(width, height)
            val shad = cb.shadow
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val p = gradientPerturbFractal(300, frequency = 0.8, position = Vector3(x/320.0,y/240.0, seconds))
                        val d = simplex(300, p.x, p.y, p.z).absoluteValue
                        shad[x, y] = ColorRGBa(d, d, d, 1.0)
                    }
                }
                shad.upload()
                drawer.image(cb)
            }
        }
    }
}