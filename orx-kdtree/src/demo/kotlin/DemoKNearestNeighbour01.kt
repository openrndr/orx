import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.kdtree.findKNearest
import org.openrndr.extra.kdtree.vector2Mapper
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
            val tree = buildKDTree(points, 2, ::vector2Mapper)

            extend {
                drawer.circles(points, 5.0)

                val kNearest = findKNearest(tree, mouse.position, k=7, dimensions = 2, ::vector2Mapper)
                drawer.fill = ColorRGBa.RED
                drawer.stroke = ColorRGBa.RED
                drawer.strokeWeight = 2.0
                drawer.circles(kNearest, 7.0)
                drawer.lineSegments(kNearest.map { LineSegment(mouse.position, it) })
            }
        }
    }
}