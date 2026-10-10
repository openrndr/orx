
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.NodeRepulseForce
import org.openrndr.extra.force2d.bodyAreaConstraint
import org.openrndr.extra.force2d.contourToBody
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.linkLengthConstraint
import org.openrndr.extra.force2d.nodeRepulseForce
import org.openrndr.extra.force2d.nodeRepulseInterbodyForce
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

/**
 * A demo similar to DemoBendyStar03, but adding 4 bodies instead
 * of 1, and an interbody repulse force.
 *
 * Two of the bodies are given a higher repulse force strength.
 * The low compliance values in the link-length and body-area constraints
 * indicate that those constraints must be strictly enforced.
 *
 * Commented-out code can be enabled to visualize the nodes
 * and the body bounds.
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

            drawer.bounds.grid(2, 2).flatten().forEach { cell ->
                //val contour = Rectangle.fromCenter(drawer.bounds.center, 400.0).contour
                val contour = regularStar(10, 50.0, 200.0, cell.center)
                val body = contourToBody(contour, density = 10.0, linkNeighbors = 5) {
                    gravity(gravity)
                    linkLengthConstraint {
                        compliance = 1E-4
                        iterations = 2
                    }
                    bodyAreaConstraint {
                        compliance = 0.0
                        iterations = 3
                    }
                    nodeRepulseForce {
                        searchRadius = 45.0
                        strength = 0.0
                    }
                    rectangularBoundsConstraint {
                        bounds = drawer.bounds.offsetEdges(-10.0)
                    }
                }
                sim.bodies.add(body)
            }

            sim.context = Dispatchers.IO
            sim.nodeRepulseInterbodyForce {
                searchRadius = 30.0
                strength = 1000.0
            }

            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate( 1.0 / 60.0, 10)
                }

                (sim.bodies[0].forces.getOrNull(1) as? NodeRepulseForce)?.strength = 10.0 //* mouse.position.y / height.toDouble()
                (sim.bodies[1].forces.getOrNull(1) as? NodeRepulseForce)?.strength = 10.0 //* mouse.position.y / height.toDouble()

                for (body in sim.bodies) {
                    body.updateBounds()
                    val points = body.nodes.map { it.position }
                    val segments = body.boundaryLinks.map {
                        val link = body.links[it]
                        LineSegment(points[link.source], points[link.target])
                    }
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.fill = ColorRGBa.WHITE
                    //drawer.circles(points, 4.0)
                    drawer.lineSegments(segments)

                    // show body bounds
                    //drawer.fill = null
                    //drawer.stroke = ColorRGBa.RED
                    //drawer.rectangle(body.bounds)
                }

                drawer.defaults()
            }
        }
    }
}