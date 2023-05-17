import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.delegatemagic.smoothing.following
import org.openrndr.extra.delegatemagic.smoothing.smoothing
import org.openrndr.math.Vector2
import kotlin.random.Random

/**
 * Demonstrates using delegate-magic tools with
 * [Double] and [Vector2].
 *
 * The white circle's position uses [following].
 * The red circle's position uses [smoothing].
 *
 * `following` uses physics (velocity and acceleration).
 * `smoothing` eases values towards the target.
 *
 * Variables using delegates (`by`) interpolate
 * toward target values, shown as gray lines.
 *
 * The behavior of the delegate-magic functions can be configured
 * via arguments that affect their output.
 *
 * The arguments come in pairs of similar name:
 * The first one, often of type [Double], is constant,
 * The second one contains `Property` in its name and can be
 * modified after its creation and even be linked to a UI
 * to modify the behavior of the delegate function in real time.
 * The `Property` argument overrides the other.
 */
fun main() = application {
    program {
        val target = object {
            var pos = drawer.bounds.center
        }

        val spos by smoothing(target::pos)
        val fpos by following(target::pos)

        extend {
            if (frameCount % 90 == 0) {
                target.pos = Vector2(
                    Random.nextDouble(0.0, width.toDouble()),
                    Random.nextDouble(10.0, height.toDouble())
                )
            }
            drawer.fill = ColorRGBa.WHITE
            drawer.circle(fpos, 15.0)

            drawer.fill = ColorRGBa.RED
            drawer.circle(spos, 10.0)

            drawer.fill = null
            drawer.stroke = ColorRGBa.GRAY.opacify(0.5)
            drawer.lineSegment(0.0, target.pos.y, width.toDouble(), target.pos.y)
            drawer.lineSegment(target.pos.x, 0.0, target.pos.x, height.toDouble())
        }
    }
}
