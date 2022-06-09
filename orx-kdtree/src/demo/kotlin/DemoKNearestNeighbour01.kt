import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment

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