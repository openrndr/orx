package rbf

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.saturate
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.math.rbf.Rbf3DInterpolator
import org.openrndr.extra.math.rbf.rbfGaussian
import org.openrndr.extra.math.rbf.rbfGaussianDerivative
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shaderphrases.noise.fhash12Phrase
import org.openrndr.extra.shaderphrases.rbf.rbfGaussianPhrase
import org.openrndr.math.Vector3
import kotlin.random.Random

/**
 * Demonstrates using a three-dimensional Radial Basis Function (RBF) interpolator for color grading.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")

        extend {
            val r = Random((seconds / 1).toInt())

            // I split points into a hull set and an inner set
            val hullPoints = listOf(
                Vector3(0.0, 0.0, 0.0),
                Vector3(1.0, 0.0, 0.0),
                Vector3(0.0, 1.0, 0.0),
                Vector3(1.0, 1.0, 0.0),
                Vector3(0.0, 0.0, 1.0),
                Vector3(1.0, 0.0, 1.0),
                Vector3(0.0, 1.0, 1.0),
                Vector3(1.0, 1.0, 1.0),
            )

            val innerPoints =
                (0 until 8).map {
                    Vector3.uniform(Vector3(0.1, 0.1, 0.1), Vector3(0.9, 0.9, 0.9), r)
                }

            // The hull colors are undistorted colors
            val hullColors = hullPoints.map {
                ColorRGBa(it.x, it.y, it.z, 1.0)
            }

            // The inner colors are distorted colors
            val innerColors = innerPoints.map {
                ColorRGBa(it.x, it.y, it.z, 1.0)
                    .shiftHue<OKHSV>(Double.uniform(-180.0, 180.0, r))
                    .saturate<OKHSV>(Double.uniform(0.0, 1.0, r))
            }

            val points = hullPoints + innerPoints
            val colors = hullColors + innerColors

            // Here the `scale` and `smoothing` values are hand-tuned. When introducing more points, the scale
            // value should be increased.
            val scale = 0.25
            val interpolator = Rbf3DInterpolator(
                points,
                colors.map { doubleArrayOf(it.r, it.g, it.b) }.toTypedArray<DoubleArray>(),
                smoothing = 0.00001,
                rbf = rbfGaussian(scale),
                rbfDerivative = rbfGaussianDerivative(scale)
            )

            /**
             * Shader style that implements RBF interpolation in the fragment shader.
             */
            /**
             * Shader style that implements RBF interpolation in the fragment shader.
             */
            val ss = shadeStyle {
                fragmentPreamble = """
                |$fhash12Phrase
                |$rbfGaussianPhrase
                |float squaredDistance(vec3 p, vec3 q) { 
                |    vec3 d = p - q;
                |    return dot(d, d);
                |}
                |vec3 rbfInterpolate(vec3 p) {
                |    vec3 c = p_mean;
                |    for (int i = 0; i < p_weights_SIZE; ++i) {
                |       float r = rbfGaussian(squaredDistance(p_points[i], p), p_scale);
                |       c += p_weights[i].rgb * r;
                |   }
                |   return c;
                |}
                """.trimMargin()

                fragmentTransform = """
                x_fill.rgb = rbfInterpolate(x_fill.rgb);
                
            """.trimIndent()
                val weights = (0 until points.size).map {
                    Vector3(interpolator.weights[it][0], interpolator.weights[it][1], interpolator.weights[it][2])
                }.toTypedArray()
                parameter("weights", weights)
                parameter("points", points.toTypedArray())
                parameter("mean", Vector3(interpolator.mean[0], interpolator.mean[1], interpolator.mean[2]))
                parameter("scale", scale)
            }

            drawer.isolated {
                drawer.shadeStyle = ss
                drawer.imageFit(image, drawer.bounds)
            }
        }
    }
}