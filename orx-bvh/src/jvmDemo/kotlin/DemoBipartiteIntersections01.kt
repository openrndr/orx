import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.bvh.BVHNode2D
import org.openrndr.extra.bvh.findIntersectingPairs
import org.openrndr.extra.noise.scatter
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to quickly find intersecting or overlapping circles between two collections of circles using
 * BVH (Bounding Volume Hierarchy) data structures.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            // Create two collections of scattered circles
            val circles = drawer.bounds.scatter(7.0).map { Circle(it, 5.0) }.shuffled()
                //.filter { it.center.distanceTo(drawer.bounds.center) < 300.0 }
            val circles2 = drawer.bounds.scatter(6.0).map { Circle(it, 5.0) }.shuffled()
                //.filter { it.center.distanceTo(drawer.bounds.center) > 100.0 }

            // Creates two `BVHNode2D` data structures by feeding them small Rectangles containing each circle
            val bvh = runBlocking {
                BVHNode2D.fromObjects(circles) { Rectangle.fromCenter(it.center, it.radius * 2.0, it.radius * 2.0) }
            }
            val bvh2 = runBlocking {
                BVHNode2D.fromObjects(circles2) { Rectangle.fromCenter(it.center, it.radius * 2.0, it.radius * 2.0) }
            }

            extend {
                // Call `findIntersectionPairs()` with both bvh data structures to find all intersections between
                // the rectangles.
                val start = System.currentTimeMillis()
                val intersections = findIntersectingPairs(bvh, bvh2)
                val end = System.currentTimeMillis()

                // For all the found intersections between rectangles, collect only the pairs where the
                // circles intersect, not just their bounding rectangles.
                val ci = intersections.flatMap {
                    val a = circles[it.first]
                    val b = circles2[it.second]
                    if (a.center.distanceTo(b.center) < a.radius + b.radius) listOf(a, b) else listOf()
                }

                println("intersections took ${end - start} ms (${circles.size} vs ${circles2.size} circles, ${ci.size} intersections)")

                drawer.stroke = null

                // In red, render the intersecting circles slightly enlarged
                drawer.fill = ColorRGBa.RED
                drawer.circles(ci.map { it.copy(radius = it.radius + 2.0) })

                // Draw the two original circle collections in green and blue
                drawer.fill = ColorRGBa.GREEN
                drawer.circles(circles)
                drawer.fill = ColorRGBa.BLUE
                drawer.circles(circles2)
            }
        }
    }
}