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
 * Entry point for an application demonstrating the use of color matrix transformations on an image.
 *
 * The program initializes a graphical application with a resolution of 720x720 pixels
 * and processes an image to display it in a series of grid cells, applying a hue shift
 * transformation based on the index of each cell.
 *
 * Key features:
 * - Loads an image from a specified file path.
 * - Configures the drawing area to consist of a horizontal grid with 16 cells.
 * - Applies a color tint transformation utilizing the red channel, shifting its hue progressively
 *   per cell index to create a colorful gradient effect.
 * - Adjusts the positions of the images within each grid cell for aesthetic alignment.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            val cells = drawer.bounds.grid(16, 1).flatten()
            for ((index, cell) in cells.withIndex()) {
                drawer.drawStyle.colorMatrix = colorMatrix {
                    tint(ColorRGBa.RED.shiftHue<OKHSV>(index * 360 / 16.0))
                }
                drawer.imageFit(image, cell, horizontalPosition = -1.0 +  2.0 * index / 15.0)
            }
        }
    }
}