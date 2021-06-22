import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.ClipMode
import org.openrndr.shape.drawComposition

suspend fun main() {
    application {
        program {
            val cd = drawComposition {
                fill = null
                circle(width / 2.0, height / 2.0, 100.0)

                fill = ColorRGBa.BLACK
                clipMode = ClipMode.REVERSE_DIFFERENCE
                circle(width / 2.0 + 50.0, height / 2.0, 100.0)
            }


            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.composition(cd)
            }
        }
    }
}