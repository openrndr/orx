import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.Arc
import org.openrndr.extra.shapes.primitives.grid

/**
 * Visualizes the ColorBrewer2 color palettes with 8 colors as circles
 * made of colored arcs.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val palettes = colorBrewer2Palettes(numberOfColors = 8)
        // Make a grid and discard some cells if there are more cells than palettes
        val grid = drawer.bounds.grid(6, 6).flatten().take(palettes.size)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            // For each grid cell
            grid.forEachIndexed { i, rect ->
                // Find the corresponding palette
                val palette = palettes[i].colors
                // And display its colors on thick arcs
                palette.forEachIndexed { ci, color ->
                    drawer.strokeWeight = 15.0
                    drawer.stroke = color
                    drawer.fill = null
                    drawer.contour(
                        Arc(
                            rect.center, rect.width * 0.35,
                            360.0 * (ci + 0.0) / palette.size,
                            360.0 * (ci + 1.0) / palette.size
                        ).contour
                    )
                }
            }
        }
    }
}
