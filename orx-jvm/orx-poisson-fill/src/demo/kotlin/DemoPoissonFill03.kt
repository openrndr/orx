import org.openrndr.ExtensionStage
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fx.Post
import org.openrndr.math.Polar
import org.openrndr.poissonfill.PoissonFill

/**
 * Demonstrates how to draw graphics not affected by a `Post` extension
 * by including them in an `extend(stage = ExtensionStage.AFTER_DRAW) { ... }` block
 * **before** the `Post` effect.
 */
fun main() = application {
    program {
        extend(stage = ExtensionStage.AFTER_DRAW) {
            drawer.stroke = ColorRGBa.WHITE.opacify(0.9)
            drawer.fill = null
            drawer.strokeWeight = 6.0
            drawer.circle(drawer.bounds.center, 150.0)
        }

        extend(Post()) {
            val pf = PoissonFill()
            post { input, output ->
                pf.apply(input, output)
            }
        }

        extend {
            drawer.stroke = null
            drawer.clear(ColorRGBa.TRANSPARENT)

            drawer.fill = ColorRGBa.RED
            drawer.circle(Polar(60.0 * seconds, 200.0).cartesian + drawer.bounds.center, 20.0)

            drawer.fill = ColorRGBa.BLUE
            drawer.circle(Polar(-60.0 * seconds, 200.0).cartesian + drawer.bounds.center, 20.0)
        }
    }
}
