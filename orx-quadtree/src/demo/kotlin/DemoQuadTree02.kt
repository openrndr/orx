import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.rectangleBatch
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.gaussian
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.extra.quadtree.Quadtree

fun main() {
    application {
        configure {
            width = 800
            height = 800
            title = "QuadTree"
        }
        program {
            val box = Rectangle.fromCenter(Vector2(400.0), 750.0)

            val points = (0 until 100).map {
                Vector2.gaussian(box.center, Vector2(95.0), Random.rnd)
            }

            val quadTree = Quadtree<Vector2>(box) { it }

            for (point in points) {
                quadTree.insert(point)
            }

            val selected = points[3]
            val radius = 40.0

            val nearestQuery = quadTree.nearest(selected, radius)

            val batch = drawer.rectangleBatch {
                this.fill = null
                this.stroke = ColorRGBa.GRAY
                this.strokeWeight = 0.5
                quadTree.batch(this)
            }

            extend {
                drawer.clear(ColorRGBa.BLACK)

                drawer.rectangles(batch)

                drawer.fill = ColorRGBa.PINK.opacify(0.7)
                drawer.stroke = null
                drawer.circles(points, 5.0)

                nearestQuery?.let { (nearest, neighbours, nodes) ->
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.YELLOW.opacify(0.2)

                    for (node in nodes) {
                        node.draw(drawer)
                    }

                    drawer.fill = ColorRGBa.GREEN.opacify(0.7)
                    drawer.circles(neighbours, 5.0)

                    drawer.fill = ColorRGBa.RED.opacify(0.9)
                    drawer.circle(nearest, 5.0)

                    drawer.fill = ColorRGBa.PINK
                    drawer.circle(selected, 5.0)

                    drawer.stroke = ColorRGBa.PINK
                    drawer.fill = null
                    drawer.circle(selected, radius)
                }
            }
        }
    }
}