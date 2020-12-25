import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {

            val cs = Rectangle(0.0, 0.0, 200.0, 200.0).contour
            val cc = Circle(100.0, 0.0, 100.0).contour

            extend {
                drawer.fill = ColorRGBa.GRAY
                drawer.stroke = ColorRGBa.PINK
                drawer.isolated {
                    drawer.contour(cs)
                    drawer.translate(300.0, 0.0)

                    // this should create a contour similar to the input contour
                    drawer.contour(cs.sampleEquidistant(4))
                    drawer.contour(cs.sampleEquidistant(3))
                }

                drawer.isolated {
                    drawer.translate(.0, 400.0)
                    drawer.contour(cc)
                    drawer.translate(300.0, 0.0)

                    drawer.contour(cc)
                    // this should draw a hexagon
                    drawer.contour(cc.sampleEquidistant(6))
                    // this should draw a triangle
                    drawer.contour(cc.sampleEquidistant(3))
                }
            }
        }
    }
}