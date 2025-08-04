import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.resourceUrl
import java.net.URL

fun main() = application {
    program {
        class Animation: Keyframer() {
            val position by Vector2Channel(arrayOf("x", "y"))
        }
        val animation = Animation()
        animation.loadFromJson(URL(resourceUrl("/demo-simple-01.json")))
        extend {
            animation(seconds)
            drawer.circle(animation.position, 100.0)
        }
    }
}