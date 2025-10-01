import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.drawImage
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.primitives.grid

/**
 * This program uses `drawer.imageFit()` to draw images using nested grid layout.
 * The main grid features 4 columns for the `Cover`, `Contain`, `Fill` and `None` fit methods,
 * and two rows for portrait and landscape images.
 * Each of those 8 cells feature a 3x3 grid, with cells combining `left`, `center` and `right` alignment
 * with `top`, `center` and `bottom` alignment.
 *
 * The image drawn in each cell is a simple image with a white background and two touching circles:
 * a pink one and a gray one. In some of the cells part of this image is cropped out (due to the fit method used).
 * In other cells the image does not fully cover the available area, revealing a dark gray background.
 */
fun main() = application {
    configure {
        width = 1600
        height = 900
    }

    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 18.0)

        // Create an image with a gray and a pink circle
        fun makeImage(cols: Int, rows: Int, side: Int = 400) = drawImage(cols * side, rows * side) {
            clear(ColorRGBa.WHITE)
            stroke = null
            bounds.grid(cols, rows).flatten().forEachIndexed { i, it ->
                fill = if (i % 2 == 0) ColorRGBa.PINK else ColorRGBa.GRAY
                circle(it.center, side / 2.0)
            }
        }

        val namedImages = mapOf(
            "portrait" to makeImage(1, 2),
            "landscape" to makeImage(2, 1)
        )
        val fitMethods = FitMethod.entries.toTypedArray()

        val grid = drawer.bounds.grid(fitMethods.size, namedImages.size, 30.0, 30.0, 30.0, 30.0)

        extend {
            drawer.fontMap = font
            drawer.stroke = null
            fitMethods.forEachIndexed { y, fitMethod ->
                namedImages.entries.forEachIndexed { x, (layoutName, img) ->
                    val cell = grid[x][y]
                    // In each grid cell draw 9 fitted images combining
                    // [left, center, right] and [top, center, bottom] alignment
                    val subgrid = cell.grid(3, 3, 0.0, 0.0, 4.0, 4.0)
                    subgrid.forEachIndexed { yy, rects ->
                        rects.forEachIndexed { xx, rect ->
                            // Draw a dark background
                            drawer.fill = ColorRGBa.WHITE.shade(0.25)
                            drawer.rectangle(rect)

                            // Draw the image using `imageFit`
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