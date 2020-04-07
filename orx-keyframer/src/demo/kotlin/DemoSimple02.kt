import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.resourceUrl
import java.net.URL

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