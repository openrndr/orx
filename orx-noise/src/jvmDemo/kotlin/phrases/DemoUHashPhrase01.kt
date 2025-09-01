package phrases

import org.openrndr.application
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.shaderphrases.noise.fhash13Phrase

/**
 * Demonstrate the use of a uniform hashing function phrase in a ShadeStyle.
 *
 * The hashing function uses the screen coordinates and the current time to
 * calculate the brightness of each pixel.
 *
 * Multiple GLSL hashing functions are defined in orx-shader-phrases.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        extend {
            /** A custom shadestyle */
            val ss = shadeStyle {
                fragmentPreamble = """
                        $fhash13Phrase
                    """.trimIndent()

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
