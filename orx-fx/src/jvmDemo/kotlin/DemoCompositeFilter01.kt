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

fun main() = application {
    program {
        extend(Post()) {
            // -- create a color buffer and fill it with random direction vectors
            val direction = colorBuffer(width, height, type = ColorType.FLOAT32)
            val s = direction.shadow
            val n = simplex2D.bipolar().fbm().scaleShiftInput(0.01, 0.0, 0.01, 0.0).withVector2Output()
            val ng = simplex2D.unipolar().scaleShiftInput(0.005, 0.0, 0.005, 0.0)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val a = smoothstep(0.4, 0.6, cos((x + y) * 0.01) * 0.5 + 0.5)
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
                    perpendicular = false
                }
                secondParameters {
                    window = 3
                    perpendicular = true
                }
            }

            val grain = FilmGrain()
            grain.grainStrength = 1.0

            // -- create a grain-blur composite filter
            val grainBlur = grain.then(bidirectional)

            post { input, output ->
                grainBlur.apply(arrayOf(input, direction), output)
            }
        }

        val image = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.image(image)
        }
    }
}
