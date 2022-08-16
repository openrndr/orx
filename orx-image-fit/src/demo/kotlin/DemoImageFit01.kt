import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadFont
import org.openrndr.draw.renderTarget
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.grid

/**
 * Tests `drawer.imageFit()` with all FitMethods for portrait and landscape images.
 */
fun main() = application {
    configure {
        width = 1600
        height = 900
    }

    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 18.0)

        // Create a test image with circles
        fun makeImage(cols: Int, rows: Int, side: Int = 400): ColorBuffer {
            val rt = renderTarget(cols * side, rows * side) {
                colorBuffer()
            }
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.WHITE)
                stroke = null
                ortho(rt)
                bounds.grid(cols, rows).flatten().forEachIndexed { i, it ->
                    fill = if (i % 2 == 0) ColorRGBa.PINK else ColorRGBa.GRAY
                    circle(it.center, side / 2.0)
                }
            }
            return rt.colorBuffer(0)
        }

        val layouts = mapOf(
            "portrait" to makeImage(1, 2),
            "landscape" to makeImage(2, 1)
        )
        val fitMethods = FitMethod.values()

        val grid = drawer.bounds.grid(fitMethods.size, layouts.size, 30.0, 30.0, 30.0, 30.0)

        extend {
            drawer.fontMap = font
            drawer.stroke = null
            fitMethods.forEachIndexed { y, fitMethod ->
                layouts.entries.forEachIndexed { x, (layoutName, img) ->
                    val cell = grid[x][y]
                    // In each grid cell draw 9 fitted images combining
                    // [left, center, right] and [top, center, bottom] alignment
                    val subgrid = cell.grid(3, 3, 0.0, 0.0, 4.0, 4.0)
                    subgrid.forEachIndexed { yy, rects ->
                        rects.forEachIndexed { xx, rect ->
                            drawer.fill = ColorRGBa.WHITE.shade(0.25)
                            drawer.rectangle(rect)
                            drawer.imageFit(img, rect, xx - 1.0, yy - 1.0, fitMethod)
                        }
                    }
                    drawer.fill = ColorRGBa.WHITE
                    drawer.text("${fitMethod.name}, $layoutName", cell.position(0.0, 1.038).toInt().vector2)
                }
            }
        }
    }
}