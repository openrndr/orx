import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.resourceUrl
import java.net.URL

/**
 * Demonstrates loading a JSON file with keyframe animation information
 * and applying it to the position of a circle.
 *
 * The JSON file contains times, coordinates, easing functions and envelopes.
 */
fun main() = application {
    program {
        class Animation: Keyframer() {
            val position by Vector2Channel(arrayOf("x", "y"))
        }
        val animation = Animation()
        animation.loadFromJson(URL(resourceUrl("/demo-envelope-01.json")))
        extend {
            animation(seconds)
            drawer.circle(animation.position, 100.0)
        }
    }
}