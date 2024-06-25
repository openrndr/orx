/**
 * Demonstrate the Contour filter
 * @author Edwin Jakobs
 */

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.edges.Contour
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.primitives.grid

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val contour = Contour()
        contour.levels = 4.0
        contour.window = 1
        contour.outputBands = true
        contour.contourColor = ColorRGBa.PINK
        contour.backgroundOpacity = 0.0

        val edges = image.createEquivalent()
        extend {
            val cells = drawer.bounds.grid(2, 2).flatten()
            val actions = listOf(
                {
                    contour.outputBands = true
                    contour.levels = 2.0
                },
                {
                    contour.outputBands = false
                    contour.levels = 2.0
                },
                {
                    contour.outputBands = false
                    contour.levels = 8.0
                },
                {
                    contour.outputBands = true
                    contour.levels = 8.0
                },
            )
            for ((cell, action) in cells zip actions) {
                action()
                contour.apply(image, edges)
                drawer.imageFit(edges, cell)
            }
        }
    }
}