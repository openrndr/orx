import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.nodeCollisionConstraint
import org.openrndr.extra.force2d.nodeContourConstraint
import org.openrndr.extra.force2d.nodeRepulseForce
import org.openrndr.extra.force2d.pointsToBody
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.math.Polar

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

//            extend(ScreenRecorder()) {
//                contentScale = 1.0
//                maximumDuration = 10.0
//                frameRate = 60.0
//                frameSkip = 60*10
//            }

            val sim = ForceSimulation()

            val gravity = GravityForce()


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
                    contour = hobbyCurve( drawer.bounds.offsetEdges(-100.0).scatter(50.0).hilbertOrder(), true)
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
                    6
                }
                val circles = sim.bodies[0].nodes.map { it.position }
                drawer.circles(circles, 10.0)
            }
        }

    }
}