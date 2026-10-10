import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.bodyAreaConstraint
import org.openrndr.extra.force2d.contourToBody
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.linkLengthConstraint
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.LineSegment

/**
 * Demonstrates how to set up an interactive physics-driven scene with a 5-point star that is affected by gravity
 * and collides with a rectangular boundary. Instead of always pointing down, gravity points from the center
 * of the window towards the mouse position.
 *
 * The simulation is instantiated via [ForceSimulation] (see the
 * [api docs](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-force-simulation/index.html))
 * We need to add bodies to the simulation. A way to create a body is by using [contourToBody]
 * [api docs](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/contour-to-body.html).
 *
 * Note how when creating the body we enable `gravity` and three
 * constraints: link-length, body-area and rectangular-bounds.
 *
 * `simulate()` is a suspending function, therefore we need to use `runBlocking`.
 * It takes two arguments: the time delta in milliseconds, and the number of
 * `substeps` to control the simulation precision.
 *
 * The simulation engine does not actually work with contours or segments,
 * but with nodes instead. That's why for rendering we read the positions out
 * of the nodes and construct [LineSegment]s out of them.
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