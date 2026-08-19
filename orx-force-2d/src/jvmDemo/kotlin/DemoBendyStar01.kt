import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.bodyAreaConstraint
import org.openrndr.extra.force2d.contourToBody
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.linkLengthConstraint
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.LineSegment

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val sim = ForceSimulation()
            val gravity = GravityForce()

            val contour = regularStar(5, 50.0, 300.0, drawer.bounds.center)
            val body = contourToBody(contour, linkNeighbors = 15) {
                gravity(gravity)
                linkLengthConstraint {
                    compliance = 5E-3
                    iterations = 1
                }
                bodyAreaConstraint {
                    compliance = 1E-5
                    iterations = 1
                }
                rectangularBoundsConstraint {
                    bounds = drawer.bounds.offsetEdges(-10.0)
                }
            }
            sim.bodies.add(body)

            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }

                for (body in sim.bodies) {
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