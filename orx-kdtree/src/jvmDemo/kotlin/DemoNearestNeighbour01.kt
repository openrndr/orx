import org.openrndr.application
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2

/**
 * Initializes an interactive graphical application that displays 1000 randomly distributed 2D points
 * on a canvas of dimensions 1280x720. The points are organized into a KD-tree for efficient spatial querying.
 *
 * Key functionality:
 * - Displays the points as small circles on the canvas.
 * - Dynamically highlights the nearest point to the cursor's position by drawing a larger circle around it.
 *
 * Highlights:
 * - KD-tree structure enables efficient nearest-neighbor searches.
 * - The nearest point to the cursor is determined and visually emphasized in real-time as the cursor moves.
 */
fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val points = MutableList(1000) {
                Vector2(Math.random() * width, Math.random() * height)
            }
            val tree = points.kdTree()
            extend {
                drawer.circles(points, 5.0)
                val nearest = tree.findNearest(mouse.position)
                nearest?.let {
                    drawer.circle(it.x, it.y, 20.0)
                }
            }
        }
    }
}