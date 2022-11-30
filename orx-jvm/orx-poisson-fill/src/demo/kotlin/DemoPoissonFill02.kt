import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fx.Post
import org.openrndr.math.Polar
import org.openrndr.poissonfill.PoissonFill

fun main() = application {
    program {
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
