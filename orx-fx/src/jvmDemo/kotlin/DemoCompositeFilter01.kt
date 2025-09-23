import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.DirectionalBlur
import org.openrndr.extra.fx.composite.then
import org.openrndr.extra.fx.grain.FilmGrain
import org.openrndr.extra.noise.*
import org.openrndr.math.smoothstep
import kotlin.math.cos
import kotlin.math.sin

/**
 * Advanced demonstration of composite filters, created by chaining
 * several filters together using the `.then()` operator.
 *
 * The demo applies a [FilmGrain] effect and a [DirectionalBlur] effect twice
 * with different parameters.
 *
 * The [DirectionalBlur] requires a color buffer to define the displacement
 * directions. In this program, the direction color buffer is populated by writing
 * into its `shadow` property pixel by pixel.
 *
 * Notice the use of `frameCount` and `seconds` to animate the effects.
 *
 * The composite effect is installed as a post-processing effect
 * using `extend(Post())`, so anything drawn in following `extend`
 * blocks is affected by it.
 */
fun main() = application {
    program {
        // -- create a color buffer and fill it with random direction vectors
        val direction = colorBuffer(width, height, type = ColorType.FLOAT32)
        val s = direction.shadow
        val n = simplex2D.bipolar().fbm().scaleShiftInput(0.01, 0.0, 0.01, 0.0).withVector2Output()
        val ng = simplex2D.unipolar().scaleShiftInput(0.005, 0.0, 0.005, 0.0)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val nv = n(2320, x.toDouble(), y.toDouble()) * smoothstep(
                    0.45,
                    0.55,
                    ng(1032, x.toDouble(), y.toDouble())
                )
                s[x, y] = ColorRGBa(nv.x, nv.y, 0.0, 1.0)
            }
        }
        s.upload()

        val directional = DirectionalBlur()

        // -- create a bidirectional composite filter by using a directional filter twice
        val bidirectional = directional.then(directional) {
            firstParameters {
                window = 50
                spread = 1.5 + sin(seconds)
                perpendicular = false
            }
            secondParameters {
                window = 3
                spread = 1.5 + cos(seconds)
                perpendicular = true
            }
        }

        val grain = FilmGrain()
        grain.grainStrength = 1.0

        // -- create a grain-blur composite filter
        val grainBlur = bidirectional.then(grain)

        extend(Post()) {
            post { input, output ->
                grain.time = frameCount * 1.0
                grainBlur.apply(arrayOf(input, direction), output)
            }
        }

        val image = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.image(image)
        }
    }
}
