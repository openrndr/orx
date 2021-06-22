import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shadestyles.NPointGradient
import org.openrndr.math.Polar
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos

/**
 * Demonstrate using an n-point gradient.
 * The gradient has 8 points in screen coordinates and 8 colors.
 * The colors are fixed, the points of the gradient move.
 * A contour is drawn using the same points from the gradient,
 * but note that this is not necessary: you can animate the gradient
 * on a static shape (a circle for example) or you can animate a shape
 * with a static gradient.
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
            val gradient = NPointGradient(Array(numPoints) {
                ColorXSVa(it * 360.0 / numPoints, 1.0, 1.0).toRGBa()
            })

            extend {
                drawer.run {
                    clear(ColorRGBa.WHITE.shade(0.9))
                    val t = PI * 2 * (frameCount % 300) / 300.0
                    val points = Array(numPoints) {
                        val lfo = cos(it * PI / 2 - t)
                        val theta = it * 360.0 / numPoints - 22.5 * lfo
                        val radius = 200 + 170 * lfo
                        bounds.center + Polar(theta, radius).cartesian
                    }
                    gradient.points = points
                    shadeStyle = gradient
                    stroke = ColorRGBa.WHITE
                    strokeWeight = 4.0
                    contour(ShapeContour.fromPoints(points.asList(), true))
                }
            }
        }
    }
}