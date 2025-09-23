import org.openrndr.application
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import kotlin.math.cos

/**
 * Demonstrates how to create an `extend` block to apply a post-processing effect.
 * The effect is an [ApproximateGaussianBlur] and its `sigma` parameter
 * is animated. The Blur effect is combined with whatever the user draws
 * in the regular `extend` block using the `Add` filter, resulting in
 * an additive composition.
 *
 * This demo also shows how to make a program window resizable.
 */
fun main() = application {
    configure {
        windowResizable = true
    }
    program {
        extend(Post()) {
            val blur = ApproximateGaussianBlur()
            val add = Add()
            post { input, output ->
                blur.window = 50
                blur.sigma = 50.0 * (cos(seconds) * 0.5 + 0.5)
                blur.apply(input, intermediate[0])
                add.apply(arrayOf(input, intermediate[0]), output)
            }
        }
        extend {
            drawer.circle(width / 2.0, height / 2.0, 100.0)
        }
    }
}

