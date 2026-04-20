package rbf

import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.drawImage
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.math.rbf.Rbf2DInterpolator
import org.openrndr.extra.math.rbf.rbfGaussian
import org.openrndr.extra.math.rbf.rbfGaussianDerivative
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shaderphrases.noise.fhash12Phrase
import org.openrndr.extra.shaderphrases.rbf.rbfGaussianPhrase
import org.openrndr.math.Vector3

/**
 * Demonstrates using a two-dimensional Radial Basis Function (RBF) interpolator for glitching.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = drawImage(width, height, contentScale = 1.0) {
            loadImage("demo-data/images/image-001.png").use {
                drawer.imageFit(it, drawer.bounds)
            }
        }
        image.shadow.download()

        val points = drawer.bounds.offsetEdges(-10.0).scatter(7.0)

        val colors = points.map {

            val ix = it.x.toInt()
            val iy = it.y.toInt()
            image.shadow[ix, iy]
        }

        // Here the `scale` and `smoothing` values are hand-tuned
        val scale = 0.035

        val interpolator = Rbf2DInterpolator(
            points,
            colors.map { doubleArrayOf(it.r, it.g, it.b) }.toTypedArray<DoubleArray>(),
            smoothing = 1E-5,
            rbf = rbfGaussian(scale),
            rbfDerivative = rbfGaussianDerivative(scale)
        )

        val ss = shadeStyle {
            fragmentPreamble = """
                |$fhash12Phrase
                |$rbfGaussianPhrase
                |float squaredDistance(vec2 p, vec2 q) { 
                |    vec2 d = p - q;
                |    return dot(d, d);
                |}
                |vec3 rbfInterpolate(vec2 p) {
                |    vec3 c = p_mean;
                |    for (int i = 0; i < p_weights_SIZE; ++i) {
                |       float r = rbfGaussian(squaredDistance(p_points[i], p), $scale);
                |       float step = p_step;
                |       r = smoothstep(step - p_width / 2.0, step + p_width / 2.0, r) * 0.1;
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
            parameter("step", mouse.position.y / height)
            parameter("width", mouse.position.x / width)
            parameter("weights", weights)
            parameter("points", points.toTypedArray())
            parameter("mean", Vector3(interpolator.mean[0], interpolator.mean[1], interpolator.mean[2]))
        }
        extend(Camera2D())
        extend {
            drawer.isolated {
                ss.parameter("step", 0.85)
                ss.parameter("width", 0.1)
                drawer.shadeStyle = ss
                drawer.rectangle(drawer.bounds)
            }
        }
    }
}