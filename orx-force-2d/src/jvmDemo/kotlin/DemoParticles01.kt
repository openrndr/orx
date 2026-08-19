import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.nodeCollisionConstraint
import org.openrndr.extra.force2d.pointsToBody
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.noise.scatter

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

            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate( 1.0 / 60.0, 10)
                }
                val circles = sim.bodies[0].nodes.map { it.position }
                drawer.circles(circles, 10.0)
            }
        }
    }
}