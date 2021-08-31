import org.openrndr.application
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.kdtree.findNearest
import org.openrndr.extra.kdtree.vector2Mapper
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
            val tree = buildKDTree(points, 2, ::vector2Mapper)
            extend {
                drawer.circles(points, 5.0)
                val nearest = findNearest(tree, mouse.position, 2, ::vector2Mapper)
                nearest?.let {
                    drawer.circle(it.x, it.y, 20.0)
                }
            }
        }
    }
}