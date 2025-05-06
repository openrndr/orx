import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.IntVector3

/**
 * This program demonstrates how to change the rendering type from POINTS to LINE_LOOP.
 * In everything else, it's identical to compute04.kt
 *
 * Output: vertexBuffer -> LINE_LOOP
 */

fun main() = application {
    program {
        val particleCount = 48000
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

        // Create a vertex buffer.
        // Padding is required if position has less than 4 dimensions.
        // val vb = vertexBuffer(vertexFormat {
        //   position(3)
        //   paddingFloat(1)
        // }, particleCount)

        // With BufferAlignment.STD430 padding is taken care of
        val vb = vertexBuffer(vertexFormat(BufferAlignment.STD430) {
            position(3)
        }, particleCount)

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
                b_vb.vertex[id].position.xy = b_particles.particle[id].pos;
            """.trimIndent()
            workGroupSize = IntVector3(32, 1, 1)
        }

        // Execute initCS
        initCS.buffer("particles", particleSSBO)
        initCS.execute(particleCount)

        extend {
            updateCS.buffer("vb", vb.shaderStorageBufferView())
            updateCS.buffer("particles", particleSSBO)
            updateCS.parameter("windowSize", drawer.bounds.dimensions)
            updateCS.execute(particleCount)

            drawer.fill = ColorRGBa.WHITE
            drawer.vertexBuffer(vb, DrawPrimitive.LINE_LOOP)
        }
    }
}