import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.bvh.BVHNode2D
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
            val circles = drawer.bounds.scatter(10.0).map { Circle(it, 10.0) }.shuffled()
            val bvh =
                withContext(Dispatchers.IO) {
                    BVHNode2D.fromObjects(circles) {
                        Rectangle.fromCenter(
                            it.center,
                            it.radius * 2.0,
                            it.radius * 2.0
                        )
                    }
                }

            extend {
                fun drawBvh(node: BVHNode2D) {
                    drawer.rectangle(node.bounds)
                    if (node.left != null) drawBvh(node.left!!)
                    if (node.right != null) drawBvh(node.right!!)
                }

                drawer.circles(circles)
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                val q = Rectangle.fromCenter(mouse.position, 140.0, 140.0)
                val found = bvh.queryRectangle(q)

                drawer.rectangle(q)
                drawer.fill = ColorRGBa.RED

                drawer.circles(found.map { circles[it] })


                //drawBvh(bvh)

            }
        }
    }
}