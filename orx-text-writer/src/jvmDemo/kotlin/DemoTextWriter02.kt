import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.textwriter.writer
import kotlin.math.PI
import kotlin.math.cos

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