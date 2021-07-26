import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.simplex1D
import org.openrndr.extra.noise.simplex2D
import org.openrndr.extra.noise.simplex3D
import org.openrndr.extra.noise.withVector2Output
import org.openrndr.extra.noise.gradient
import org.openrndr.shape.contour

suspend fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        val n = simplex3D.withVector2Output().gradient()
        extend {
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.lineJoin = LineJoin.ROUND
            drawer.stroke = ColorRGBa.WHITE
            for (y in 0 until height step 20) {
                for (x in 0 until width step 20) {
                    val d = n(40, x * 0.003, y * 0.003,seconds) * 5.0
                    drawer.lineSegment(x * 1.0, y * 1.0, x * 1.0 + d.x, y * 1.0 + d.y)
                }
            }
        }
    }
}