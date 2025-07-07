import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.IntVector3

/**
 * This program is almost identical to compute04.kt, but instead of writing into
 * a vertex buffer, it draws the particles into a 2D image (a ColorBuffer).
 *
 * Output: 2D Image
 */

fun main() = application {
    program {
        val particleCount = 4800
        // Define SSBO format
        val fmt = shaderStorageFormat {
            struct("Particle", "particle", particleCount) {
                primitive("pos", BufferPrimitiveType.VECTOR2_FLOAT32)
                primitive("velocity", BufferPrimitiveType.VECTOR2_FLOAT32)
            }
        }
        println("Study the padding in the format:\n$fmt\n")

        // Create SSBO
        val particleSSBO = shaderStorageBuffer(fmt)

        // Create a color buffer to write into.
        val cb = colorBuffer(width, height, type = ColorType.FLOAT32)

        // Create Compute Shaders
        val initCS = computeStyle {
            computeTransform = """
                uint id = gl_GlobalInvocationID.x;
                b_particles.particle[id].pos = vec2(320.0, 240.0);
                b_particles.particle[id].velocity = vec2(cos(id), sin(id));                
            """.trimIndent()
            workGroupSize = IntVector3(32, 1, 1)
        }
        val updateCS = computeStyle {
            computePreamble = """
                void updateParticle(inout Particle p) {
                    // Add velocity to position
                    p.pos += p.velocity;
                    
                    // Deal with the particle trying to escape the window
                    if(p.pos.x < 0.0) {
                      p.pos.x = 0.0;
                      p.velocity.x = abs(p.velocity.x); 
                    }
                    if(p.pos.y < 0.0) {
                      p.pos.y = 0.0;
                      p.velocity.y = abs(p.velocity.y); 
                    }
                    if(p.pos.x > p_windowSize.x) {
                      p.pos.x = p_windowSize.x;
                      p.velocity.x = -abs(p.velocity.x); 
                    }
                    if(p.pos.y > p_windowSize.y) {
                      p.pos.y = p_windowSize.y;
                      p.velocity.y = -abs(p.velocity.y); 
                    }                                  
                }
            """.trimIndent()
            computeTransform = """
                // The id of the element being currently processed
                uint id = gl_GlobalInvocationID.x;
                updateParticle(b_particles.particle[id]);

                // write into the image
                imageStore(p_img, ivec2(b_particles.particle[id].pos.xy), vec4(1.0));
            """.trimIndent()
            workGroupSize = IntVector3(32, 1, 1)
        }
        // Execute initCS
        initCS.buffer("particles", particleSSBO)
        initCS.execute(particleCount, initCS.workGroupSize.x)

        extend {
            // Clear the image, otherwise all pixels become eventually white
            cb.fill(ColorRGBa.TRANSPARENT)

            // Pass image to the compute shader.
            // We can choose between READ, READ_WRITE or WRITE.
            updateCS.image("img", cb.imageBinding(0, ImageAccess.WRITE))
            updateCS.buffer("particles", particleSSBO)
            updateCS.parameter("windowSize", drawer.bounds.dimensions)
            updateCS.execute(particleCount / updateCS.workGroupSize.x)

            drawer.image(cb)
        }
    }
}