import org.openrndr.application
import org.openrndr.extra.fcurve.MultiFCurve
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.extra.fcurve.vector2

/**
 * Demonstrates the use of `MultiFCurve`, which enables
 * combining multiple `FCurve` definitions and produce
 * one or more variables of various types, including
 * `Vector2` (in this example), `ColorRGBa` and more.
 *
 * This demo produces the same animation as found in
 * `DemoFCurve01.kt` and `DemoFCurve02.kt`, but instead
 * of sampling the `x` and `y` curves separately, they
 * are returned as a `Vector2` for convenience.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        class XYAnimation : MultiFCurve(
            mapOf(
                "x" to fcurve("M0 Q4,360,5,720"),
                "y" to fcurve("M360 h5")
            )
        ) {
            val position = vector2("x", "y")
        }

        val xyAnimation = XYAnimation()
        val position = xyAnimation.position.sampler()

        extend {
            drawer.circle(position(seconds.mod(5.0)), 100.0)
        }
    }
}
