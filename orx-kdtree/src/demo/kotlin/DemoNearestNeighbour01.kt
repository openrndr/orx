import org.openrndr.application
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2

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