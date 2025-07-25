import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.math.IntVector3

/**
 * This program demonstrates
 * - how to use a compute shader and an SSBO to do many computations in parallel
 * - how to use a compute shader to initialize an SSBO
 * - how to use a different shader to update the SSBO
 *
 * Note the `workGroupSize` property. The GPU splits tasks
 * into chunks and computes those in parallel. The ideal workGroupSize depends on
 * the GPU being used. Too small of a size may be inefficient.
 *
 * In some cases a compute shader works with 2D images or 3D data structures, but in this
 * program we are processing the elements of a 1D array. That's why we only
 * increase the x value to 32, leaving y and z equal to 1.
 *
 * Note: this program only does the computation, but does not visualize the results
 * in any way. We will do that in another program.
 *
 * Output: none
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
        val particlesSSBO = shaderStorageBuffer(fmt)

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
            computeTransform = """
                // The id of the element being currently processed
                uint id = gl_GlobalInvocationID.x;
                
                // Add velocity to position
                b_particles.particle[id].pos += b_particles.particle[id].velocity;
                
                // Deal with the particle trying to escape the window
                if(b_particles.particle[id].pos.x < 0.0) {
                  b_particles.particle[id].pos.x = 0.0;
                  b_particles.particle[id].velocity.x = abs(b_particles.particle[id].velocity.x); 
                }
                if(b_particles.particle[id].pos.y < 0.0) {
                  b_particles.particle[id].pos.y = 0.0;
                  b_particles.particle[id].velocity.y = abs(b_particles.particle[id].velocity.y); 
                }
                if(b_particles.particle[id].pos.x > p_windowSize.x) {
                  b_particles.particle[id].pos.x = p_windowSize.x;
                  b_particles.particle[id].velocity.x = -abs(b_particles.particle[id].velocity.x); 
                }
                if(b_particles.particle[id].pos.y > p_windowSize.y) {
                  b_particles.particle[id].pos.y = p_windowSize.y;
                  b_particles.particle[id].velocity.y = -abs(b_particles.particle[id].velocity.y); 
                }                
            """.trimIndent()
            workGroupSize = IntVector3(32, 1, 1)
        }

        // Execute initCS
        initCS.buffer("particles", particlesSSBO)
        initCS.execute(particleCount / initCS.workGroupSize.x)

        extend {
            updateCS.buffer("particles", particlesSSBO)
            updateCS.parameter("windowSize", drawer.bounds.dimensions)
            updateCS.execute(particleCount / updateCS.workGroupSize.x)
        }
    }
}