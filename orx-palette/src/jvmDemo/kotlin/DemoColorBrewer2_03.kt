import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.Arc
import org.openrndr.extra.shapes.primitives.grid

/**
 * Visualizes 49 ColorBrewer2 color palettes of type "Diverging" as circles
 * made of colored arcs. Since there are more palettes than grid cells,
 * not all palettes are visualized.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val palettes = colorBrewer2Palettes(paletteType = ColorBrewer2Type.Diverging)
        val grid = drawer.bounds.grid(7, 7).flatten().take(palettes.size)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            grid.forEachIndexed { i, rect ->
                val palette = palettes[i].colors
                palette.forEachIndexed { ci, color ->
                    drawer.strokeWeight = 15.0
                    drawer.stroke = color
                    drawer.fill = null
                    drawer.contour(
                        Arc(
                            rect.center, rect.width * 0.4,
                            360.0 * (ci + 0.0) / palette.size,
                            360.0 * (ci + 1.0) / palette.size
                        ).contour
                    )
                }
            }
        }
    }
}
