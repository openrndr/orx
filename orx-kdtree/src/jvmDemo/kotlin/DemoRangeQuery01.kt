import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2


/**
 * Initializes an interactive graphical application that demonstrates spatial querying with KD-trees.
 * A canvas is populated with 1000 randomly distributed 2D points, and a KD-tree is used for efficient
 * spatial operations. The program dynamically highlights points within a specified radius from the
 * user's cursor position.
 *
 * Key features:
 * - Generates and displays 1000 random 2D points within the canvas.
 * - Builds a KD-tree structure for optimized querying of spatial data.
 * - Dynamically highlights points within a specified radius (50.0) from the cursor position.
 * - Visualizes the current query radius around the cursor as an outline circle.
 * - Uses different fill and stroke styles to distinguish highlighted points and query visuals.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val points = MutableList(1000) {
            Vector2(Math.random() * width, Math.random() * height)
        }
        val tree = points.kdTree()
        val radius = 50.0

        extend {
            drawer.circles(points, 5.0)

            val allInRange = tree.findAllInRadius(mouse.position, radius = radius)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.PINK
            drawer.strokeWeight = 2.0
            drawer.circles(allInRange, 7.0)

            drawer.fill = null
            drawer.strokeWeight = 1.0
            drawer.circle(mouse.position, radius)
        }
    }
}
