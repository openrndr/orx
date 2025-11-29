import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.textwriter.writer

/**
 * This demo shows how to align texts to the left, center, right, top, center and bottom of a container box.
 *
 * It creates a grid of 3x3 cells to demonstrate all alignment combinations by setting the
 * `style.verticalAlign` and the `style.horizontalAlign` to 0.0, 0.5 and 1.0.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val columns = drawer.bounds.grid(3, 3).flatten()
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
                for ((index, column) in columns.withIndex()) {
                    drawer.isolated {
                        drawer.stroke = ColorRGBa.WHITE
                        drawer.fill = null
                        drawer.rectangle(column.offsetEdges(-10.0))
                    }

                    writer {
                        style.verticalAlign = (index / 3) / 2.0
                        style.horizontalAlign = index.mod(3) / 2.0
                        box = column.offsetEdges(-20.0)
                        text(
                            listOf(
                                "DEAR FRIENDS.",
                                "IT TOOK A WHILE, BUT NOW WE HAVE TEXT ALIGNMENT IN BOTH DIRECTIONS!",
                                "LET'S CELEBRATE"
                            )
                        )
                    }
                }
            }
        }
    }
}