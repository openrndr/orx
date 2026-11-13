import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.bvh.BVHNode2D
import org.openrndr.extra.bvh.findIntersectingPairs
import org.openrndr.extra.noise.scatter
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to quickly find overlapping circles in a collection of scattered circles using BVH.
 *
 * The circles are placed with a scatter radius of 5.0, but the circle radii are actually 6.0, so some
 * overlap will likely happen.
 *
 * The BVH data structure is created out of the rectangular bounding boxes of the circles.
 *
 * The `findIntersectingPairs()` is used to find all intersections between those bounding boxes.
 * Next, the program creates a list of circles that actually intersect, not just their bounding boxes.
 *
 * The original circles are then rendered white, and the intersecting ones in red.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            extend {
                val circles = drawer.bounds.scatter(5.0).map { Circle(it, 6.0) }

                val start = System.currentTimeMillis()
                val bvh = runBlocking {
                    // setting the context here makes a difference as the BVH is built using concurrency
                    withContext(Dispatchers.IO) {
                        BVHNode2D.fromObjects(circles) {
                            Rectangle.fromCenter(
                                it.center,
                                it.radius * 2.0,
                                it.radius * 2.0
                            )
                        }
                    }
                }

                val intersections = findIntersectingPairs(bvh)
                val end = System.currentTimeMillis()
                println("intersections took ${end - start} ms")

                val ci = intersections.flatMap {
                    if (circles[it.first].center.distanceTo(circles[it.second].center) < circles[it.first].radius + circles[it.second].radius)
                        listOf(circles[it.first], circles[it.second]) else listOf()
                }

                drawer.circles(circles)
                drawer.fill = ColorRGBa.RED
                drawer.circles(ci)
            }
        }
    }
}