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
import org.openrndr.extra.math.rbf.rbfGaussian
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shaderphrases.noise.fhash12Phrase
import org.openrndr.extra.shaderphrases.rbf.rbfGaussianPhrase
import org.openrndr.math.Vector3
import kotlin.collections.indices
import kotlin.collections.map
import kotlin.collections.toTypedArray
import kotlin.random.Random
import kotlin.ranges.until
import kotlin.text.trimIndent
import kotlin.text.trimMargin

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val r = Random(0)
            val points = drawer.bounds.offsetEdges(-100.0).uniform(14, r)

            val colors = (0 until points.size).map {
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
                rbf = rbfGaussian(scale)
            )

            /**
             * Shader style that implements RBF interpolation in the fragment shader.
             * Uses Gaussian RBF function to interpolate colors between given points.
             * Includes custom distance calculation and color interpolation functions.
             */
            val ss = shadeStyle {
                fragmentPreamble = """${fhash12Phrase}
                    |${rbfGaussianPhrase}
                    |float squaredDistance(vec2 p, vec2 q) { 
                    |    vec2 d = p - q;
                    |    return dot(d, d);
                    |}
                    |vec3 rbfInterpolate(vec2 p) {
                    |    vec3 c = p_mean;
                    |    for (int i = 0; i < p_weights_SIZE; ++i) {
                    |       float r = rbfGaussian(squaredDistance(p_points[i], p), $scale);
                    |       c.r += p_weights[i].r * r;
                    |       c.g += p_weights[i].g * r;
                    |       c.b += p_weights[i].b * r;
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