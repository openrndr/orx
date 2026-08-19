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
                    if (circles[it.first].center.distanceTo(circles[it.second].center) < circles[it.first].radius + circles[it.second].radius) listOf(circles[it.first], circles[it.second]) else listOf()
                }

                drawer.circles(circles)
                drawer.fill = ColorRGBa.RED
                drawer.circles(ci)
            }
        }
    }
}