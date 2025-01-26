import org.openrndr.application
import org.openrndr.color.ColorXSVa
import org.openrndr.color.rgb
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.shadestyles.NPointLinearGradient
import org.openrndr.extra.shadestyles.NPointLinearGradientOKLab
import org.openrndr.shape.Rectangle
import kotlin.math.pow
import kotlin.math.sin

/**
 * Demonstrate using a multicolor linear gradient.
 * The gradient has 8 static saturated colors.
 * The positions of the colors are first distributed
 * uniformly between 0.0 and 1.0 and then animated towards one of
 * the ends over time using pow() and sin(seconds).
 */
fun main() = application {
    program {
        val numPoints = 8
        // Create gradients using two different color spaces
        val gradients = listOf(
            NPointLinearGradient(Array(numPoints) {
                ColorXSVa(it * 360.0 / numPoints, 1.0, 1.0).toRGBa()
            }),
            // OKLab is better at maintaining luminosity across the gradient
            NPointLinearGradientOKLab(Array(numPoints) {
                ColorXSVa(it * 360.0 / numPoints, 1.0, 1.0).toRGBa()
                    .toOKLABa()
            })
        )

        extend {
            // The points should be sorted values between 0.0 and 1.0
            val distribution = Array(numPoints) {
                // uniform distribution
                // (it / (numPoints - 1.0))

                // skewed and animated distribution
                (it / (numPoints - 1.0)).pow(1.0 + 0.5 * sin(seconds))
            }
            drawer.run {
                clear(rgb(0.2))
                stroke = rgb(0.35)
                strokeWeight = 8.0

                gradients.forEachIndexed { i, gradient ->
                    shadeStyle = gradient
                    gradient.points = distribution

                    gradient.rotation = seconds * 10
                    circle(bounds.position(0.34, 0.29 + 0.44 * i), 110.0)

                    gradient.rotation += 90
                    rectangle(
                        Rectangle.fromCenter(
                            bounds.position(0.655, 0.29 + 0.44 * i), 200.0
                        )
                    )
                }
            }
        }
    }
}
