import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.color.rgb
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shadestyles.NPointLinearGradient
import org.openrndr.shape.Rectangle
import kotlin.math.pow
import kotlin.math.sin

/**
 * Demonstrate using a multi color linear gradient.
 * The gradient has 8 static saturated colors.
 * The positions of the colors are first distributed
 * uniformly between 0.0 and 1.0 and then animated towards one of
 * the ends over time using pow() and sin(seconds).
 */
suspend fun main() {
    application {
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            val numPoints = 8
            val gradient = NPointLinearGradient(Array(numPoints) {
                ColorXSVa(it * 360.0 / numPoints, 1.0, 1.0).toRGBa()
            })

            extend {
                drawer.run {
                    clear(rgb(0.2))
                    // The points should be sorted values between 0.0 and 1.0
                    gradient.points = Array(numPoints) {
                        // uniform distribution
                        // (it / (numPoints - 1.0))

                        // skewed and animated distribution
                        (it / (numPoints - 1.0)).pow(1.0 + 0.5 * sin(seconds))
                    }
                    gradient.rotation = seconds * 10
                    shadeStyle = gradient
                    stroke = rgb(0.35)
                    fill = ColorRGBa.WHITE
                    strokeWeight = 8.0

                    gradient.rotation = seconds * 10
                    circle(bounds.position(0.34, 0.5), 110.0)

                    gradient.rotation += 90
                    rectangle(Rectangle.fromCenter(
                            bounds.position(0.655, 0.5), 200.0, 200.0))
                }
            }
        }
    }
}