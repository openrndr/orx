package compute

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.IntVector3

/**
 * This program demonstrates
 * - how to modify the particles, adding color, age and ageVelocity.
 * - make the particles wrap around the edges instead of bouncing on them.
 *
 * Output: 2D Image
 */

fun main() = application {
    program {
        val particleCount = 48000
        // Define SSBO format
        val fmt = shaderStorageFormat {
            struct("Particle", "particle", particleCount) {
                primitive("pos", BufferPrimitiveType.VECTOR2_FLOAT32)
                primitive("velocity", BufferPrimitiveType.VECTOR2_FLOAT32)
                primitive("color", BufferPrimitiveType.VECTOR3_FLOAT32)
                primitive("age", BufferPrimitiveType.FLOAT32)
                primitive("ageVelocity", BufferPrimitiveType.FLOAT32)
            }
        }
        println("Study the padding in the format:\n$fmt\n")

        // Create SSBO
        val particleSSBO = shaderStorageBuffer(fmt)

        // Create a color buffer to write into.
        val cb = colorBuffer(width, height, type = ColorType.FLOAT32)

        // Create Compute Shaders
        val initCS = computeStyle {
            computePreamble = """
            // From lygia.xyz
            vec3 hue2rgb(const in float hue) {
                float R = abs(hue * 6.0 - 3.0) - 1.0;
                float G = 2.0 - abs(hue * 6.0 - 2.0);
                float B = 2.0 - abs(hue * 6.0 - 4.0);
                return clamp(vec3(R,G,B), 0.0, 1.0);
            }                
            void initParticle(uint id, inout Particle p) {
                float k = 100.0 / $particleCount;
                p.velocity = vec2(cos(id * k), sin(id * k));
                p.pos = vec2(320.0, 240.0) + p.velocity * id * 0.0003;
                p.color = hue2rgb(fract(id * k / 4.0));
                p.age = id * k / 5.0;
                p.ageVelocity = sin(id * k * 1.0) * 0.1 + 0.2;
            }
            """.trimIndent()
            computeTransform = """
                uint id = gl_GlobalInvocationID.x;
                initParticle(id, b_particles.particle[id]);
            """.trimIndent()
            workGroupSize = IntVector3(32, 1, 1)
        }
        val updateCS = computeStyle {
            computePreamble = """
                void updateParticle(inout Particle p) {
                    // Add velocity to position
                    p.pos += p.velocity;
                    
                    // Update age
                    p.age += p.ageVelocity;
                    
                    // Deal with the particle trying to escape the window
                    if(p.pos.x < 0.0) {
                      p.pos.x += p_windowSize.x;
                    }
                    if(p.pos.y < 0.0) {
                      p.pos.y += p_windowSize.y;
                    }
                    if(p.pos.x > p_windowSize.x) {
                      p.pos.x = 0.0; 
                    }
                    if(p.pos.y > p_windowSize.y) {
                      p.pos.y = 0.0; 
                    }                                  
                }
            """.trimIndent()
            computeTransform = """
                // The id of the element being currently processed
                uint id = gl_GlobalInvocationID.x;
                updateParticle(b_particles.particle[id]);
                                                                                
                float alpha = sin(b_particles.particle[id].age);
                alpha = sin(alpha + sin(alpha)) * 0.5 + 0.5;
                                
                // draw particle in the image
                imageStore(p_img, 
                    ivec2(b_particles.particle[id].pos), 
                    vec4((b_particles.particle[id].color * alpha), alpha)
                );                
            """.trimIndent()
        }

        // Execute initCS
        initCS.buffer("particles", particleSSBO)
        initCS.execute(particleCount)

        extend {
            // Clear the image, otherwise all pixels become eventually white
            cb.fill(ColorRGBa.TRANSPARENT)

            // Pass image to the compute shader.
            // We can choose between READ, READ_WRITE or WRITE.
            updateCS.image("img", cb.imageBinding(0, ImageAccess.WRITE))
            updateCS.buffer("particles", particleSSBO)
            updateCS.parameter("windowSize", drawer.bounds.dimensions)
            updateCS.execute(particleCount)

            drawer.image(cb)
        }
    }
}