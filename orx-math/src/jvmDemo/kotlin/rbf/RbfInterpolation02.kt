package rbf

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.spaces.OKLab
import org.openrndr.extra.color.tools.shadeLuminosity
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.math.rbf.Rbf2DInterpolator
import org.openrndr.extra.math.rbf.rbfInverseMultiQuadratic
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shaderphrases.noise.fhash12Phrase
import org.openrndr.extra.shaderphrases.rbf.rbfInverseMultiQuadraticPhrase
import org.openrndr.math.Vector3
import kotlin.random.Random

/**
 * Demonstrates using a two-dimensional Radial Basis Function (RBF) interpolator
 * with the user provided 2D input points, their corresponding values (colors in this demo),
 * a smoothing factor, and a radial basis function kernel.
 *
 * The program chooses 20 random points in the window area leaving a 100 pixels
 * margin around the borders and assigns a randomized color to each point.
 *
 * Next it creates the interpolator using those points and colors, a smoothing factor
 * and the RBF function used for interpolation. This function takes a squared distance
 * as input and returns a scalar value representing the influence of points at that distance.
 *
 * A ShadeStyle implementing the same RBF interpolation is created next, used to render
 * the background gradient interpolating all points and their colors.
 *
 * After rendering the background, the original points and their colors are
 * drawn as circles for reference.
 *
 * Finally, the current mouse position is used for sampling a color
 * from the interpolator and displayed for comparison. Notice that even if
 * the fill color is flat, it may look like a gradient due to the changing
 * colors in the surrounding pixels.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val r = Random(0)
            val points = drawer.bounds.offsetEdges(-100.0).uniform(20, r)

            val colors = points.map {
                ColorRGBa.PINK
                    .shiftHue<OKHSV>(Double.uniform(-180.0, 180.0, r))
                    .shadeLuminosity<OKLab>(Double.uniform(0.4, 1.0, r))
                    .toLinear()
            }

            // Here the `scale` and `smoothing` values are hand-tuned
            val scale = 0.04 / 5.0
            val interpolator = Rbf2DInterpolator(
                points,
                colors.map { doubleArrayOf(it.r, it.g, it.b) }.toTypedArray<DoubleArray>(),
                smoothing = 0.09,
                rbf = rbfInverseMultiQuadratic(scale)
            )

            /**
             * Shader style that implements RBF interpolation in the fragment shader.
             * Uses an Inverse MultiQuadratic RBF function to interpolate colors between given points.
             * Includes custom distance calculation and color interpolation functions.
             */
            val ss = shadeStyle {
                fragmentPreamble = """
                    |$fhash12Phrase
                    |$rbfInverseMultiQuadraticPhrase
                    |float squaredDistance(vec2 p, vec2 q) { 
                    |    vec2 d = p - q;
                    |    return dot(d, d);
                    |}
                    |vec3 rbfInterpolate(vec2 p) {
                    |    vec3 c = p_mean;
                    |    for (int i = 0; i < p_weights_SIZE; ++i) {
                    |       float r = rbfInverseMultiQuadratic(squaredDistance(p_points[i], p), $scale);
                    |       c += p_weights[i].rgb * r;
                    |   }
                    |   return c;
                    |}
                    """.trimMargin()

                fragmentTransform = """
                    x_fill.rgb = rbfInterpolate(c_boundsPosition.xy * vec2(720.0, 720.0));
                    
                """.trimIndent()
                val weights = (0 until points.size).map {
                    Vector3(interpolator.weights[it][0], interpolator.weights[it][1], interpolator.weights[it][2])
                }.toTypedArray()
                parameter("weights", weights)
                parameter("points", points.toTypedArray())
                parameter("mean", Vector3(interpolator.mean[0], interpolator.mean[1], interpolator.mean[2]))
            }
            extend {
                // draw the interpolated colors
                drawer.isolated {
                    drawer.shadeStyle = ss
                    drawer.rectangle(drawer.bounds)
                }

                // draw the original points and colors for reference
                drawer.circles {
                    for (i in points.indices) {
                        fill = colors[i]
                        circle(points[i], 10.0)
                    }
                }

                // compute color on CPU for comparison
                drawer.fill = interpolator.interpolate(mouse.position).let {
                    ColorRGBa(it[0], it[1], it[2], 1.0)
                }
                drawer.circle(mouse.position, 30.0)
            }
        }
    }
}