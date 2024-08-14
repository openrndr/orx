//import org.openrndr.application
//import org.openrndr.color.ColorRGBa
//import org.openrndr.draw.ColorBuffer
//import org.openrndr.draw.isolatedWithTarget
//import org.openrndr.draw.loadFont
//import org.openrndr.draw.renderTarget
//import org.openrndr.extra.runway.*
//import org.openrndr.math.smoothstep
//
///**
// * This demonstrates an image reinterpretation effect.
// * This example requires a `runway/BigBiGAN` model to be active in Runway.
// */
//
//fun main() = application {
//    configure {
//        width = 512
//        height = 256
//    }
//
//    program {
//        val rt = renderTarget(256, 256) {
//            colorBuffer()
//        }
//        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 256.0)
//        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
//        extend {
//            drawer.isolatedWithTarget(rt) {
//                drawer.clear(ColorRGBa.BLACK)
//                drawer.ortho(rt)
//                val it = seconds.toInt()
//                val t = seconds - it
//                drawer.fill = ColorRGBa.PINK.shade(smoothstep(0.0, 0.2, t) * smoothstep(1.0, 0.8, t))
//                drawer.fontMap = font
//                drawer.text("" + alphabet[seconds.toInt()], 64.0, 128.0 + 64)
//            }
//            val result: BigBiGANResult =
//                    runwayQuery("http://localhost:8000/query", BigBiGANQuery(rt.colorBuffer(0).toData()))
//
//            val image = ColorBuffer.fromData(result.outputImage)
//            drawer.image(rt.colorBuffer(0))
//            drawer.image(image, 256.0, 0.0)
//            image.destroy()
//        }
//    }
//}