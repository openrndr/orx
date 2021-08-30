    import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

fun main() {
    application {
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            var points = poissonDiskSampling(200.0, 200.0, 5.0, 10)

            val rectPoints = points.map { Circle(Vector2(100.0, 100.0) + it, 3.0) }

            points = poissonDiskSampling(200.0, 200.0, 5.0, 10, true) { w: Double, h: Double, v: Vector2 ->
                Circle(Vector2(w, h) / 2.0, 100.0).contains(v)
            }

            val circlePoints = points.map { Circle(Vector2(350.0, 100.0) + it, 3.0) }

            extend {
                drawer.clear(ColorRGBa.BLACK)

                drawer.stroke = null
                drawer.fill = ColorRGBa.PINK
                drawer.circles(rectPoints)
                drawer.circles(circlePoints)
            }
        }
    }
}