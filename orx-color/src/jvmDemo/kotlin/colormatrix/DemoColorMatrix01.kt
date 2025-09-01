package colormatrix

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.colormatrix.colorMatrix
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.primitives.grid

/**
 * This demo modifies the displayed image in each grid cell
 * using color matrix transformations to demonstrate color channel inversions based on
 * the grid cell's index. The image is adjusted to fit within each grid cell while maintaining
 * alignment.
 *
 * Functionality:
 * - Loads an image from the specified file path.
 * - Splits the drawing area into an evenly spaced 4x2 grid.
 * - Applies different color matrix inversions (red, green, blue) based on the position index.
 * - Fits the image into each grid cell while providing horizontal alignment adjustments.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            val cells = drawer.bounds.grid(4, 2).flatten()
            for ((index, cell) in cells.withIndex()) {
                drawer.drawStyle.colorMatrix = colorMatrix {
                    invert(index and 1 != 0, index and 2 != 0, index and 4 != 0)
                }
                drawer.imageFit(image, cell, horizontalPosition = -0.5)
            }
        }
    }
}