//import org.openrndr.application
//import org.openrndr.color.ColorRGBa
//import org.openrndr.extensions.SingleScreenshot
//import org.openrndr.extra.compositor.compose
//import org.openrndr.extra.compositor.draw
//import org.openrndr.extra.compositor.layer
//import org.openrndr.extra.compositor.post
//import org.openrndr.extra.fx.blur.GaussianBloom
//import org.openrndr.extra.fx.blur.LaserBlur
//import org.openrndr.extra.gui.GUI
//import org.openrndr.extra.gui.addTo
//import org.openrndr.extra.noise.simplex
//import org.openrndr.math.Vector2
//import kotlin.math.absoluteValue
//
//suspend fun main() = application {
//    configure {
//        width = 1280
//        height = 720
//    }
//
//    program {
//        if (System.getProperty("takeScreenshot") == "true") {
//            extend(SingleScreenshot()) {
//                this.outputFile = System.getProperty("screenshotPath")
//            }
//        }
//
//        val gui = GUI()
//        val c = compose {
//            layer {
//                draw {
//                    drawer.fill = null
//                    drawer.strokeWeight = 4.0
//                    drawer.translate(width/2.0, height/2.0)
//                    drawer.rotate(seconds*45.0 + simplex(0, seconds)*45.0)
//                    drawer.translate(-width/2.0, -height/2.0)
//                    for (y in -1..1) {
//                        for (x in -1..1) {
//                            drawer.stroke = ColorRGBa.RED.toHSVa()
//                                    .shiftHue(0.0 + simplex(500+x+y,seconds)*5.0)
//                                    .shade(0.5 + 0.5 * simplex(300+x+y,seconds*4.0).absoluteValue)
//                                    .toRGBa()
//                            val r = simplex(400+x+y, seconds) * 150.0 + 150.0
//                            drawer.circle(width / 2.0 + x * 100.0, height / 2.0 + y * 100.0, r)
//                        }
//                    }
//                }
//                post(LaserBlur()) {
//                    center = Vector2(simplex(2, seconds*0.1), simplex(100, seconds*0.1))
//                    aberration = simplex(5, seconds) * 0.01
//                    radius = simplex(7, seconds)
//                }.addTo(gui)
//                post(GaussianBloom()).addTo(gui)
//            }
//        }
//        extend(gui) {
//            doubleBind = true
//        }
//        extend {
//            c.draw(drawer)
//        }
//    }
//}