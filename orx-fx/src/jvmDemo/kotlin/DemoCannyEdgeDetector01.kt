import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.edges.CannyEdgeDetector

/**
 * Demonstrates the [CannyEdgeDetector] effect applied to a loaded
 * color photograph.
 */
fun main() = application {
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val ced = CannyEdgeDetector()
        val edges = image.createEquivalent()
        extend {
            ced.apply(image, edges)
            drawer.image(edges)
        }
    }
}