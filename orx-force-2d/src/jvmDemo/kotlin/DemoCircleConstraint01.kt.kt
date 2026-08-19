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