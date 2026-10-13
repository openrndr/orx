import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.*
import org.openrndr.extra.noise.scatter
import org.openrndr.math.Vector2

/**
 * A "hello world" of particle simulation. The particles are scattered on the drawer bounds
 * when the simulation starts.
 *
 * Mouse-controllable gravity is the only active force, and two constraints are present: a
 * `rectangularBoundsConstraint` to keep the particles inside the program window and colliding
 * with its borders, and a `nodeCollisionConstraint` to make them collide with each other.
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

            val body = pointsToBody(drawer.bounds.scatter(15.0), radius = 10.0) {
                gravity(gravity)
                nodeCollisionConstraint {
                    compliance = 1E-3
                }
                rectangularBoundsConstraint {
                    bounds = drawer.bounds.offsetEdges(-10.0)
                    bounce = 1.0
                }
            }
            sim.bodies.add(body)
            sim.context = Dispatchers.IO

            // For a better README.md screenshot
            gravity.gravity = Vector2(-50.0)
            repeat(50) { sim.simulate(1.0 / 60.0, 10) }

            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }
                val circles = sim.bodies[0].nodes.map { it.position }
                drawer.circles(circles, 10.0)
            }
        }
    }
}