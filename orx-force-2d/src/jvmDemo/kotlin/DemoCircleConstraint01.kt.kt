import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.nodeCircleConstraint
import org.openrndr.extra.force2d.nodeCollisionConstraint
import org.openrndr.extra.force2d.pointsToBody
import org.openrndr.extra.noise.scatter
import org.openrndr.shape.Circle

/**
 * This demo creates a particle simulation.
 *
 * It starts by creating a collection of `Vector2`s using the
 * `Rectangle.scatter` method, then converts them into
 * bodies by calling [pointsToBody].
 *
 * Those bodies are affected by
 * [gravity](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-gravity-force/index.html),
 * a [nodeCollisionConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-node-collision-constraint/index.html)
 * that makes bodies bump into each other,
 * and a [nodeCircleConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-node-circle-constraint/index.html)
 * which ensures bodies are within or on the boundary of a circle.
 *
 * Try increasing the `compliance` value of the node-circle constraint to relax it,
 * giving some priority to gravity. If, on the other hand, you lower that value,
 * bodies are strictly placed on the circle boundary.
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

            val pts = drawer.bounds.scatter(20.0)
            val body = pointsToBody(pts, radius = 10.0) {
                gravity(gravity)
                nodeCollisionConstraint {
                    compliance = 1E-3
                }
                nodeCircleConstraint {
                    compliance = 1.0
                    circle = { Circle(360.0, 360.0, 300.0) }
                }
            }
            sim.bodies.add(body)

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