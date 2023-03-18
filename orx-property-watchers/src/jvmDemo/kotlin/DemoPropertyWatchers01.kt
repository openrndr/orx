import org.openrndr.application
import org.openrndr.extra.propertywatchers.watchingProperty

fun main() {
    application {
        program {
            val state = object {
                val x by watchingProperty(mouse::position) {
                    it.x
                }

                val xx by watchingProperty(::x) {
                    it * it
                }
            }

            extend {
                state.x
            }
        }
    }
}