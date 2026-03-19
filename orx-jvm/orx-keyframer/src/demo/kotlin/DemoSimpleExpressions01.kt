import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.resourceUrl
import java.net.URL

/**
 * Demonstrates using simple expressions in the keyframer JSON file.
 *
 * A value called `cycleDuration` is passed via `parameters.
 * The JSON file uses this value for the duration of certain animations
 * and to specify the time of each keyframe.
 *
 * Note the multiplication operations in the JSON file.
 */
fun main() = application {
    program {
        class Animation : Keyframer() {
            val position by Vector2Channel(arrayOf("x", "y"))
            val radius by DoubleChannel("radius")
        }

        val animation = Animation()
        animation.loadFromJson(
            URL(resourceUrl("/demo-simple-expressions-01.json")),
            parameters = mapOf("cycleDuration" to 0.5)
        )
        extend {
            animation(seconds)
            drawer.circle(animation.position, animation.radius)
        }
    }
}