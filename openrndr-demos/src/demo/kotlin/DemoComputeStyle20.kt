import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import kotlin.math.sin

/**
 * Use a compute shader to read from a colorBuffer
 * and write into a different colorBuffer.
 *
 * The input colorBuffer is updated on every animation frame
 * with a scaling circle.
 *
 * Then the compute shader is executed to update every pixel
 * in the output colorBuffer by reading a displaced pixel
 * from the input colorBuffer.
 *
 * Output: 2D Image
 */
fun main() {
    application {
        program {
            val input = renderTarget(width, height) {
                colorBuffer(type = ColorType.FLOAT32)
            }
            val output = input.colorBuffer(0).createEquivalent()

            val cs = computeStyle {
                computeTransform = """                    
                    ivec2 id = ivec2(gl_GlobalInvocationID.xy);
                    ivec2 src = ivec2(id + sin(id) * p_m);
                    vec4 c = imageLoad(p_inputImage, src);
                    imageStore(p_outputImage, id, c);                    
                """.trimIndent()
            }

            cs.image("inputImage", input.colorBuffer(0).imageBinding(0, ImageAccess.READ))
            cs.image("outputImage", output.imageBinding(0, ImageAccess.WRITE))

            extend {
                // Update input
                drawer.isolatedWithTarget(input) {
                    clear(ColorRGBa.TRANSPARENT)
                    circle(bounds.center, 100.0 + 80 * sin(seconds))
                }

                // Apply the compute shader to update output
                cs.parameter("m", sin(seconds * 0.8) * 13.0 + 15.0)
                cs.execute(output.width, output.height, 1)

                // Draw result
                drawer.image(output)
            }
        }
    }
}
