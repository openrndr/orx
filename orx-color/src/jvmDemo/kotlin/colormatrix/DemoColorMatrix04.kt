package colormatrix

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.colormatrix.colorMatrix
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.primitives.grid


/**
 * Entry point of a graphical application that demonstrates the use of color matrix
 * transformations on an image displayed within a grid layout.
 *
 * Overview:
 * - Initializes a window with a resolution of 720x720 pixels.
 * - Loads an image from the specified file path.
 * - Splits the drawing canvas into a 7x1 grid of cells.
 * - In each grid cell, applies custom grayscale transformations to the image using
 *   a color matrix. The grayscale transformation coefficients for red, green, and blue
 *   channels are computed based on the index of the grid cell.
 * - Displays the adjusted image in each grid cell with horizontal alignment modifications
 *   to position the images dynamically based on their index within the grid.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            val cells = drawer.bounds.grid(7, 2)
            for ((rowIndex, row) in cells.withIndex()) {
                for ((index, cell) in row.withIndex()) {
                    drawer.drawStyle.colorMatrix = colorMatrix {
                        var r = if ((index + 1) and 1 != 0) 1.0 else 0.0
                        var g = if ((index + 1) and 2 != 0) 1.0 else 0.0
                        var b = if ((index + 1) and 4 != 0) 1.0 else 0.0
                        val sum = r + g + b
                        r /= sum
                        g /= sum
                        b /= sum
                        grayscale(r, g, b)
                        if (rowIndex == 1) {
                            invert()
                        }
                    }
                    drawer.imageFit(image, cell, horizontalPosition = -0.5)
                }
            }
        }
    }
}