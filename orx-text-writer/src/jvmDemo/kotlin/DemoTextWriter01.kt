import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.textwriter.writer
import org.openrndr.shape.Rectangle

/**
 * This demo features the drawing of a centered rectangle and the addition of styled text inside
 * the rectangle. The application manages the drawing of shapes and implementation of text rendering
 * with specific font and settings.
 *
 * The following operations are performed:
 * - A rectangle is created from the center of the drawing bounds.
 * - The rectangle is drawn without a fill and with a white stroke.
 * - A custom font is loaded and applied to the drawer.
 * - A `TextWriter` is utilized to display the text "hello world" inside the rectangle, adhering to
 *   specific styling and formatting rules.
 *
 * Key Components:
 * - `application` establishes the visual environment.
 * - `Rectangle` provides a way to define the rectangular area.
 * - `drawer` enables isolated operations for drawing elements.
 * - `writer` facilitates text rendering with alignment and spacing adjustments.
 */
fun main() {

    application {

        program {
            extend {
                val r = Rectangle.fromCenter(drawer.bounds.center, 200.0, 200.0)
                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.rectangle(r)
                }
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
                writer {
                    box = r.offsetEdges(-10.0)
                    newLine()
                    text("hello world")
                }
            }
        }
    }
}