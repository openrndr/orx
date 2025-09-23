//import org.openrndr.application
//import org.openrndr.color.ColorRGBa
//import org.openrndr.extra.compositor.compose
//import org.openrndr.extra.compositor.draw
//import org.openrndr.extra.compositor.layer
//import org.openrndr.extra.compositor.post
//import org.openrndr.extra.fx.patterns.Checkers
//import org.openrndr.extra.jumpfill.fx.InnerGlow

// Temporarily commented out until JumpFlood.kt is compatible with
// https://github.com/openrndr/openrndr/commit/9871b80161b155bf61371301ce1b7b9cf9a10adf

//fun main() = application {
//    configure {
//        width = 720
//        height = 720
//    }
//    program {
//        val c = compose {
//            layer {
//                post(Checkers())
//            }
//            layer {
//                draw {
//                    drawer.fill = ColorRGBa.PINK.shade(0.5)
//                    drawer.stroke = null
//                    drawer.circle(width / 2.0, height / 2.0, width * 0.35)
//                }
//                post(InnerGlow()) {
//                    color = ColorRGBa(1.0, 1.0, 1.0, 0.25);
//                    width = 30.0
//                }
//            }
//        }
//        extend {
//            c.draw(drawer)
//        }
//    }
//}