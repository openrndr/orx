import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.*
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.math.Polar

/**
 * Demonstration of the [NodeContourConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-node-contour-constraint/index.html).
 *
 * A random hobby-contour is created and used for this particle simulation.
 * The gravity is not interactive but rotates clockwise over time.
 * The particles then "fall" onto part of the contour depending on the direction of gravity.
 *
 * Try lowering the compliance of the `nodeContourConstraint` to make the particles follow the contour more strictly,
 * or increase the value for more looseness.
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

            val wobblyContour = hobbyCurve(drawer.bounds.offsetEdges(-100.0).scatter(50.0).hilbertOrder(), true)

            val pts = drawer.bounds.scatter(20.0)
            val body = pointsToBody(pts, radius = 10.0) {
                gravity(gravity)
                nodeCollisionConstraint {
                    compliance = 1E-3
                }
                nodeRepulseForce {
                    searchRadius = 10.0
                    strength = 10.0
                }
                nodeContourConstraint {
                    compliance = 1.0
                    contour = wobblyContour
                    strength = 0.2
                }
                rectangularBoundsConstraint {
                    bounds = drawer.bounds.offsetEdges(-10.0)
                    bounce = 1.0
                }
            }
            sim.bodies.add(body)

            extend {
                gravity.gravity = Polar(seconds * 72.0, 300.0).cartesian

                drawer.clear(ColorRGBa.BLACK)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }
                val circles = sim.bodies[0].nodes.map { it.position }
                drawer.circles(circles, 10.0)
            }
        }
    }
}