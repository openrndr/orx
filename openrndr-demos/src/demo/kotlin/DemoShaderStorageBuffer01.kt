//import org.openrndr.application
//import org.openrndr.draw.VertexElementType
//import org.openrndr.draw.shadeStyle
//import org.openrndr.draw.shaderStorageBuffer
//import org.openrndr.draw.shaderStorageFormat
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
//fun main() = application {
//    program {
//
//        val ssb = shaderStorageBuffer(shaderStorageFormat {
//            //member("foo", VertexElementType.FLOAT32, 1000)
//
//        })
//        val ss = shadeStyle {
//            buffer("someBuffer", ssb)
//            fragmentTransform = "float a = b_someBuffer.foo[0]; b_someBuffer.foo[1] += 2.0;"
//        }
//
//        val bb = ByteBuffer.allocateDirect(ssb.format.size)
//        bb.order(ByteOrder.nativeOrder())
//
//        extend {
//            ssb.clear()
//
//            drawer.shadeStyle = ss
//            drawer.circle(100.0, 100.0, 200.0)
//            bb.rewind()
//            ssb.read(bb)
//            bb.rewind()
//            val f0 = bb.float
//            val f1 = bb.float
//            println(f1)
//
//        }
//
//    }
//}