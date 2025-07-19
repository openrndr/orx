import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

/**
 * This program demonstrates
 * - How to animate 2D particles but render them as 2D triangles.
 * - How to deal with two buffers of different size: the particles buffer
 *   and the vertex buffer, which has three times more elements.
 *   The update compute shader calculates 3 vertices for each particle.
 * - How to make the init compute shader initialize both the particles (position and velocity)
 *   and the colors of the vertices.
 * - How to create a minimal ShadeStyle to set per-vertex colors,
 *   and render the colors interpolated across each triangle.
 *
 * Output: vertexBuffer -> TRIANGLES
 */

fun main() = application {
    program {
        val particleCount = 3200

        // Define SSBO format
        val ssboFmt = shaderStorageFormat {
            struct("Particle", "particle", particleCount) {
                primitive("pos", BufferPrimitiveType.VECTOR2_FLOAT32)
                primitive("velocity", BufferPrimitiveType.VECTOR2_FLOAT32)
            }
        }
        println("Study the padding in the format:\n$ssboFmt\n")

        // Create SSBO
        val particleSSBO = shaderStorageBuffer(ssboFmt)

        // The padding is required to match the expected layout.
        // Even if we are in a 2D world we need to use a 3D position
        // because that's what OPENRNDR expects in its shaders.
        val vertFormat = vertexFormat(BufferAlignment.STD430) {
            position(3)
            color(4)
        }
        println("Study the padding in the vertex buffer format:\n$vertFormat\n")

        // Create vertex buffer.
        // Note how me multiply the particleCount by 3 (three vertices per particle).
        val vb = vertexBuffer(vertFormat, particleCount * 3)

        // Create Compute Shaders
        val initCS = computeStyle {
            computeTransform = """
                uint id = gl_GlobalInvocationID.x;
                                    
                b_particles.particle[id].pos = vec2(320.0, 240.0);
                b_particles.particle[id].velocity = vec2(cos(id), sin(id));
                
                // Generate colors based on id
                vec4 col = vec4(
                    sin(id + 0.000) * 0.5 + 0.5, 
                    sin(id + 2.094) * 0.5 + 0.5, 
                    sin(id + 4.188) * 0.5 + 0.5, 1.0);
                
                // Swap R, G and B and darken the two rear vertices of each triangle.
                // This creates a gradient in each triangle.
                b_vb.vertex[id * 3 + 0].color = col.rgba;
                b_vb.vertex[id * 3 + 1].color = col.grba * 0.5;
                b_vb.vertex[id * 3 + 2].color = col.rbga * 0.25;
            """.trimIndent()
        }
        val updateCS = computeStyle {
            computePreamble = """
                const float margin = 16.0;   
                void updateParticle(inout Particle p) {
                    // Add velocity to position
                    p.pos += p.velocity;

                    // Deal with the particle trying to escape the window
                    if(p.pos.x < -margin) {
                      p.pos.x += p_windowSize.x + 2.0 * margin;
                    }
                    if(p.pos.y < -margin) {
                      p.pos.y += p_windowSize.y + 2.0 * margin; 
                    }
                    if(p.pos.x > p_windowSize.x + margin) {
                      p.pos.x = -margin; 
                    }
                    if(p.pos.y > p_windowSize.y + margin) {
                      p.pos.y = -margin; 
                    }
                }
            """.trimIndent()
            computeTransform = """
                // The id of the element being currently processed
                uint id = gl_GlobalInvocationID.x;
                updateParticle(b_particles.particle[id]);
                
                // Calculate the vertices of a directed triangle
                // pointing towards `velocity`. Hint:
                // vel (x,y) has two normals (-y, x) and (y, -x).
                vec2 pos = b_particles.particle[id].pos;
                vec2 vel = b_particles.particle[id].velocity * margin;
                vec2 n0 = vec2(-vel.y, vel.x) * 0.5;
                b_vb.vertex[id * 3 + 0].position = vec3(pos + vel, 0.0);
                b_vb.vertex[id * 3 + 1].position = vec3(pos + n0, 0.0);
                b_vb.vertex[id * 3 + 2].position = vec3(pos - n0, 0.0);
            """.trimIndent()
        }

        // Execute initCS
        initCS.buffer("vb", vb.shaderStorageBufferView())
        initCS.buffer("particles", particleSSBO)
        initCS.execute(particleCount / initCS.workGroupSize.x)

        extend {
            updateCS.buffer("vb", vb.shaderStorageBufferView())
            updateCS.buffer("particles", particleSSBO)
            updateCS.parameter("windowSize", drawer.bounds.dimensions)
            updateCS.execute(particleCount / updateCS.workGroupSize.x)

            drawer.fill = ColorRGBa.WHITE
            drawer.shadeStyle = shadeStyle {
                // The color of every triangle's pixel is interpolated using
                // its three vertex colors
                fragmentTransform = "x_fill = va_color;"
            }
            drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)
        }
    }
}