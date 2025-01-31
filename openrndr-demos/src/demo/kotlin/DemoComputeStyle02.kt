import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * A program identical to compute01.kt, except that the order of variables
 * `age` and `pos` have been swapped, resulting in a less ideal memory layout.
 * In this case the SSBO requires 192 bytes instead of 112, and padding is
 * inserted after the variables `time`, `age` and `pos`.
 *
 * Output: byteBuffer -> text
 */

fun main() = application {
    program {
        // Define SSBO format
        val fmt = shaderStorageFormat {
            primitive("time", BufferPrimitiveType.FLOAT32)
            primitive("vertex", BufferPrimitiveType.VECTOR2_FLOAT32, 3)
            struct("Particle", "particles", 5) {
                primitive("age", BufferPrimitiveType.FLOAT32)
                primitive("pos", BufferPrimitiveType.VECTOR3_FLOAT32)
            }
        }
        println("Study the padding in the format:\n$fmt\n")

        // Create SSBO
        val ssbo = shaderStorageBuffer(fmt)

        // Populate SSBO
        ssbo.put {
            write(3.0.toFloat()) // time
            repeat(3) {
                write(Vector2(1.1, 1.2)) // vertex
            }
            repeat(5) {
                write(1.0.toFloat()) // age
                write(Vector3(2.1, 2.2, 2.3))// pos
            }
        }

        // Create Compute Shader
        val cs = computeStyle {
            computeTransform = """
                b_myData.time = 3.3;
                b_myData.vertex[0] = vec2(7.01);
                b_myData.vertex[1] = vec2(7.02);
                b_myData.vertex[2] = vec2(7.03);
                b_myData.particles[0].pos = vec3(112.0);
                b_myData.particles[0].age = 111.0;                
            """.trimIndent()
        }
        cs.buffer("myData", ssbo)

        // Download SSBO data to CPU
        val byteBufferBeforeExecute = ssbo.createByteBuffer()
        byteBufferBeforeExecute.rewind()
        ssbo.read(byteBufferBeforeExecute)

        // Execute compute shader
        cs.execute(1, 1, 1)

        // Download SSBO data to CPU
        val byteBufferAfterExecute = ssbo.createByteBuffer()
        byteBufferAfterExecute.rewind()
        ssbo.read(byteBufferAfterExecute)

        // Debugging

        // Notice the (maybe unexpected) 0.0 padding values printed on the console.
        // Depending on the variable size in bytes, padding may be added by the system
        // to align them in memory. This will depend on the sizes of the involved variables,
        // and their order. For instance, a vec3 and a float do not require padding, but
        // a float followed by a vec3 pads the float with 3 values, and the vec3 with one.
        // Run compute02.kt and study the output to observe a more inefficient layout.
        byteBufferBeforeExecute.rewind()
        byteBufferAfterExecute.rewind()
        repeat(ssbo.format.size / 4) {
            println("$it: ${byteBufferBeforeExecute.float} -> ${byteBufferAfterExecute.float}")
        }
    }
}