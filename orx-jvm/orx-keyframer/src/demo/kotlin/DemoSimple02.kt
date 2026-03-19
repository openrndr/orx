import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.resourceUrl
import java.net.URL

/**
 * A minimal orx-keyframe demo. It creates a class extending `Keyframer()` and specifies the variables to control
 * (a Vector2, a Double, and a ColorRGBa in this case), instantiates this class,
 * then loads a JSON file containing animation information.
 *
 * In the `extend { }` block, the current time is passed to the `Keyframer` and its properties are used
 * to draw on the window.
 */
fun main() = application {
    program {
        class Animation: Keyframer() {
            val position by Vector2Channel(arrayOf("x", "y"))
            val radius by DoubleChannel("radius")
            val color by RGBChannel(arrayOf("r", "g", "b"))
        }
        val animation = Animation()
        animation.loadFromJson(URL(resourceUrl("/demo-simple-02.json")))
        extend {
            animation(seconds)
            drawer.fill = animation.color
            drawer.circle(animation.position, animation.radius)
        }
    }
}