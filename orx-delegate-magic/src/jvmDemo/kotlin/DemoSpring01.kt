import org.openrndr.application
import org.openrndr.extra.delegatemagic.dynamics.springForcing
import kotlin.random.Random

/**
 * Demonstrates the use of `springForcing` to animate the `x`, `y` and `radius`
 * properties of a circle simulating spring physics.
 *
 * The target values of all three properties change randomly with a 1% chance.
 * Note how the spring stiffness is higher for the `x` value.
 *
 * Since `springForcing` is a method of `Clock`, there is no need to call any
 * update methods for the values to be interpolated over time.
 */
fun main() = application {
    program {
        val state = object {
            var x = width / 2.0
            var y = height / 2.0
            var radius = 5.0
        }

        val sx by springForcing(state::x, k = 10.0)
        val sy by springForcing(state::y)
        val sradius by springForcing(state::radius)
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