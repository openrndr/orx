import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.bvh.BVHNode2D
import org.openrndr.extra.bvh.findIntersectingPairs
import org.openrndr.extra.noise.scatter
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to prepare two BVH data structures to be processed via
 * `findIntersectionPairs()`.
 */
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
                    val a = circles[it.first]
                    val b = circles2[it.second]
                    if (a.center.distanceTo(b.center) < a.radius + b.radius) listOf(a, b) else listOf()
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