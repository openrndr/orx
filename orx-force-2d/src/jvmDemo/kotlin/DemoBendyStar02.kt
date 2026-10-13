import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.*
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.LineSegment

/**
 * A simulation similar to DemoBendyStar01.kt but with some
 * changed parameters and one additional constraint:
 * node-collision.
 *
 * The four existing constraints have a `compliance` of 0.0
 * (its default value), meaning that all forces must be applied
 * with equal strictness.
 *
 * In this case the regular star has 24 points,
 * `linkNeighbors` is reduced to 5 and the `iterations`
 * of the link-length constraint increased to 3, resulting
 * in a very different behavior.
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

            val contour = regularStar(24, 50.0, 300.0, drawer.bounds.center)
            val body = contourToBody(contour, linkNeighbors = 5) {
                gravity(gravity)
                linkLengthConstraint {
                    compliance = 0.0
                    iterations = 3
                }
                bodyAreaConstraint {
                    compliance = 0.0
                    iterations = 1
                }
                nodeCollisionConstraint {
                    for (node in body.nodes) {
                        node.radius = 4.0
                    }
                }
                rectangularBoundsConstraint {
                    bounds = drawer.bounds.offsetEdges(-10.0)
                }
            }
            sim.bodies.add(body)

            // For a better README.md screenshot
            repeat(15) { sim.simulate(1.0 / 60.0, 10) }

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
                        val it = body.links[it]
                        LineSegment(points[it.source], points[it.target])
                    }
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.fill = ColorRGBa.WHITE
                    drawer.lineSegments(segments)
                }
            }
        }
    }
}