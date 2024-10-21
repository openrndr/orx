package phrases

import org.openrndr.application
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.noise.phrases.*

/**
 * Demonstrate uniform hashing function phrase in a shadestyle
 */
fun main() {
    application {
        program {
            extend {
                /** A custom shadestyle */
                val ss = shadeStyle {
                    fragmentPreamble = """$fhash13"""
                    fragmentTransform = """
                        float cf = fhash13(vec3(c_screenPosition, p_time));
                        x_fill = vec4(cf, cf, cf, 1.0);                                                                       
                    """.trimIndent()
                    parameter("time", seconds)
                }
                drawer.shadeStyle = ss
                drawer.circle(drawer.bounds.center, 100.0)
            }
        }
    }
}