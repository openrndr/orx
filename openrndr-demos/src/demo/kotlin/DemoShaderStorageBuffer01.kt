import org.openrndr.application
import org.openrndr.draw.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

// A demo of shaderStorageBuffer doing no useful work

fun main() = application {
    program {

        // Construct a SSB
        val ssb = shaderStorageBuffer(shaderStorageFormat {
            primitive("foo", BufferPrimitiveType.FLOAT32, 1000)
        })

        // A ShadeStyle that reads from and writes into an SSB
        val ss = shadeStyle {
            buffer("someBuffer", ssb)
            fragmentTransform = """
                float a = b_someBuffer.foo[0]; 
                b_someBuffer.foo[1] += 2.0;
            """.trimIndent()
        }

        // A ByteBuffer in RAM to download the GPU data into
        val bb = ByteBuffer.allocateDirect(ssb.format.size)
        bb.order(ByteOrder.nativeOrder())

        extend {
            // Clear the SSB
            ssb.clear()

            drawer.shadeStyle = ss
            drawer.circle(100.0, 100.0, 200.0)

            // Download the SSB into RAM
            bb.rewind()
            ssb.read(bb)

            bb.rewind()
            val f0 = bb.float
            val f1 = bb.float
            println(f1)
            // The shade style runs for every pix el in the circle.
            // The order in which the pixels are processed is not known
            // Therefore the value of `f1` can vary from frame to frame,
            // because we don't know how many times `+= 2.0` was executed.
        }

    }
}