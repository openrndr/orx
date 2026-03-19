import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.extra.keyframer.KeyframerFormat
import org.openrndr.resourceUrl
import java.net.URL

/**
 * Loads and plays the animation described in `demo-full-01.json`.
 * The keyframes contained in that file control the position, radius, and color of a circle.
 */
fun main() = application {
    program {
        class Animation: Keyframer() {
            val position by Vector2Channel(arrayOf("x", "y"))
            val radius by DoubleChannel("radius")
            val color by RGBChannel(arrayOf("r", "g", "b"))
        }
        val animation = Animation()
        animation.loadFromJson(URL(resourceUrl("/demo-full-01.json")), format = KeyframerFormat.FULL)
        extend {
            animation(seconds)
            drawer.fill = animation.color
            drawer.circle(animation.position, animation.radius)
        }
    }
}