import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment

/**
 * This demo initializes an interactive graphical application where 1000 randomly distributed points
 * are displayed on a 2D canvas. A KD-tree structure is used for spatial querying of the points, enabling
 * efficient nearest-neighbor searches based on the user's cursor position. The closest 7 points to the
 * cursor are highlighted with circles and lines connecting them to the cursor.
 *
 * Key features:
 * - Generates 1000 random 2D points within the canvas dimensions (1080x720).
 * - Builds a KD-tree from the list of points for optimized spatial querying.
 * - Visualizes the points and highlights the 7 nearest neighbors to the user's cursor position dynamically.
 * - Highlights include red-colored circles around the nearest points and red lines connecting them to the cursor.
 */
fun main() {
    application {
        configure {
            width = 1080
            height = 720
        }

        program {
            val points = MutableList(1000) {
                Vector2(Math.random() * width, Math.random() * height)
            }
            val tree = points.kdTree()

            extend {
                drawer.circles(points, 5.0)

                val kNearest = tree.findKNearest(mouse.position, k = 7)
                drawer.fill = ColorRGBa.RED
                drawer.stroke = ColorRGBa.RED
                drawer.strokeWeight = 2.0
                drawer.circles(kNearest, 7.0)
                drawer.lineSegments(kNearest.map { LineSegment(mouse.position, it) })
            }
        }
    }
}