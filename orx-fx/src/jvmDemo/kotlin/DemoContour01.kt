import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.edges.Contour
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.primitives.grid

/**
 * Demonstrates the [Contour] filter.
 * @author Edwin Jakobs
 *
 * This demo creates a grid of 2x2 to draw a loaded image four times,
 * each using the [Contour] effect with different parameters.
 *
 * `actions` is a variable containing a list of 4 functions.
 * Each of these functions sets the effect parameters to different values.
 *
 * The 4 grid cells and the 4 actions are used in pairs:
 * first the action is called to set the effect parameters, the
 * effect is applied, and the result is drawn in a cell.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val contour = Contour()
        contour.window = 1
        contour.contourColor = ColorRGBa.PINK
        contour.backgroundOpacity = 0.0

        val edges = image.createEquivalent()

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
        extend {
            for ((cell, action) in cells zip actions) {
                action()
                contour.apply(image, edges)
                drawer.imageFit(edges, cell)
            }
        }
    }
}