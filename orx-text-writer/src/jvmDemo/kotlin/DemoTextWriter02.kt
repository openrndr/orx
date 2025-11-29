import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.textwriter.writer
import kotlin.math.PI
import kotlin.math.cos

/**
 * Demonstrates the `writer`s `style.horizontalAlign` property,
 * which enables left (0.0), center (0.5), right (1.0) text alignment
 * and any values in between.
 *
 * The program creates a 3x3 grid of texts and interpolates their alignments
 * between left and right using the cosine of the current time in seconds.
 *
 * A time offset is included in each cell to distribute them over the
 * cosine wave, so the text lines move at different speeds and directions.
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
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
                for ((index, column) in columns.withIndex()) {
                    writer {
                        style.horizontalAlign = cos(seconds + (index/8.0) * 2 * PI) * 0.5 + 0.5
                        box = column.offsetEdges(-20.0)
                        newLine()
                        text("DEAR FRIENDS.\nIT TOOK A WHILE, BUT NOW WE HAVE HORIZONTAL TEXT ALIGNMENT!\nLET'S CELEBRATE")
                    }
                }
            }
        }
    }
}