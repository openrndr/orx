//import org.openrndr.application
//import org.openrndr.color.ColorRGBa
//import org.openrndr.extra.syphon.SyphonClient
//
///**
// * This example uses After Effects and OPENRNDR connected via Syphon
// */
//fun main() = application {
//    configure {
//        // The maximum resolution supported by the free
//        // version of AESyphon
//        width = 1024
//        height = 768
//    }
//
//    program {
//        val syphonClient = SyphonClient("Adobe After Effects", "Live Preview")
//
//        extend(syphonClient)
//        extend {
//            drawer.clear(ColorRGBa.BLACK)
//            drawer.image(syphonClient.buffer)
//        }
//    }
//}