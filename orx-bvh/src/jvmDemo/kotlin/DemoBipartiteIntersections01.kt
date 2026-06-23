import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.bvh.BVHNode2D
import org.openrndr.extra.bvh.findIntersectingPairs
import org.openrndr.extra.noise.scatter
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val circles = drawer.bounds.scatter(7.0).map { Circle(it, 5.0) }.shuffled()
            val circles2 = drawer.bounds.scatter(6.0).map { Circle(it, 5.0) }.shuffled()

            val bvh = runBlocking {
                BVHNode2D.fromObjects(circles) { Rectangle.fromCenter(it.center, it.radius * 2.0, it.radius * 2.0) }

            }
            val bvh2 = runBlocking {
                BVHNode2D.fromObjects(circles2) { Rectangle.fromCenter(it.center, it.radius * 2.0, it.radius * 2.0) }
            }

            extend {
                val start = System.currentTimeMillis()
                val intersections = findIntersectingPairs(bvh, bvh2)
                val end = System.currentTimeMillis()

                println("intersections took ${end - start} ms")

                val ci = intersections.flatMap {
                    if (circles[it.first].center.distanceTo(circles2[it.second].center) < circles[it.first].radius + circles2[it.second].radius) listOf(circles[it.first], circles2[it.second]) else listOf()
                }

                drawer.stroke = null

                drawer.fill = ColorRGBa.RED
                drawer.circles(ci.map { it.copy(radius = it.radius + 2.0) })

                drawer.fill = ColorRGBa.GREEN
                drawer.circles(circles)
                drawer.fill = ColorRGBa.BLUE
                drawer.circles(circles2)
            }
        }
    }
}