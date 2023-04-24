import org.openrndr.application
import org.openrndr.extra.delegatemagic.dynamics.springForcing
import kotlin.random.Random

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