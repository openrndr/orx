import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.distort.Lenses

/**
 * Demonstrates the [Lenses] effect, which by default subdivides a color buffer
 * in 8 columns and 6 rows, and displaces the source texture inside each rectangle.
 * Try experimenting with some of the other parameters, like `distort`.
 * You can even animate them.
 */
fun main() = application {
    configure {
        width = 640
        height = 480

    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val lenses = Lenses()
        val edges = image.createEquivalent()
        extend {
            lenses.rotation = 30.0
            lenses.scale = 1.5

            lenses.apply(image, edges)
            drawer.image(edges)
        }
    }
}