import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.force2d.*
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.LineSegment

/**
 * This version of the BendyStar demo
 * uses a 10-point start and replaces.
 * `nodeCollisionConstraint` by `nodeRepulseForce`.
 *
 * It also configures the coroutine context to enable
 * multithreading.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val sim = ForceSimulation()
            val gravity = GravityForce()

            sim.apply {
                context = Dispatchers.IO
            }

            val contour = regularStar(10, 50.0, 300.0, drawer.bounds.center)
            val body = contourToBody(contour, linkNeighbors = 3) {
                gravity(gravity)
                linkLengthConstraint {
                    compliance = 1E-4
                    iterations = 4
                }
                bodyAreaConstraint {
                    compliance = 0.0
                    iterations = 3
                }
                nodeRepulseForce {
                    searchRadius = 45.0
                    strength = 10.0
                }
                rectangularBoundsConstraint {
                    bounds = drawer.bounds.offsetEdges(-10.0)
                }
            }
            sim.bodies.add(body)

            if (System.getProperty("takeScreenshot") == "true") {
                extensions.filterIsInstance<SingleScreenshot>().forEach {
                    it.delayFrames = 30
                }
            }

            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }

                for (body in sim.bodies) {
                    body.updateBounds()
                    val points = body.nodes.map { it.position }
                    val segments = body.boundaryLinks.map {
                        val link = body.links[it]
                        LineSegment(points[link.source], points[link.target])
                    }
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.fill = ColorRGBa.WHITE
                    drawer.lineSegments(segments)
                }
            }
        }
    }
}