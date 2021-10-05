import org.openrndr.application
import org.openrndr.internal.colorBufferLoader

fun main() {
    application {
        program {
            extend {
                val proxy = colorBufferLoader.loadFromUrl("https://avatars3.githubusercontent.com/u/31103334?s=200&v=4")
                proxy.colorBuffer?.let {
                    drawer.image(it)
                }
            }
        }
    }
}