import org.openrndr.application
import org.openrndr.extra.delegatemagic.smoothing.smoothing
import kotlin.random.Random

/**
 * Demonstrates the use of the `smoothing` delegate, which interpolates
 * properties over time towards a target value.
 *
 * In this program, the state of the object is kept in an `object` with
 * three properties: `x`, `y` and `radius`.
 *
 * A second set of variables is used to track and smooth changes to
 * the `state` object: `sx`, `sy` and `sradius`. The `smoothing` factor
 * is not provided in the constructor, assuming its default value.
 *
 * The properties in the `state` object are randomly (and independently)
 * updated with a 1% probability.
 *
 * By the nature of the used interpolation, changed properties interpolate
 * first faster and then at a decreasing rate (decelerating) until
 * reaching the target value.
 */
fun main() = application {
    program {
        val state = object {
            var x = width / 2.0
            var y = height / 2.0
            var radius = 5.0
        }

        val sx by smoothing(state::x)
        val sy by smoothing(state::y)
        val sradius by smoothing(state::radius)
        extend {
            if (Random.nextDouble() < 0.01) {
                state.radius = Random.nextDouble(10.0, 200.0)
            }
            if (Random.nextDouble() < 0.01) {
                state.x = Random.nextDouble(0.0, width.toDouble())
            }
            if (Random.nextDouble() < 0.01) {
                state.y = Random.nextDouble(10.0, height.toDouble())
            }
            drawer.circle(sx, sy, sradius)
        }
    }
}