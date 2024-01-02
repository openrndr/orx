import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector2


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
}