import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.bodyAreaConstraint
import org.openrndr.extra.force2d.contourToBody
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.linkLengthConstraint
import org.openrndr.extra.force2d.naiveBroadPhaseCollisionDetector
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.force2d.sapCollisionConstraint
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.shape.Circle
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

            sim.apply {
                naiveBroadPhaseCollisionDetector()
                sapCollisionConstraint()
                context = Dispatchers.IO
            }

            //val pts = / drawer.bounds.offsetEdges(-50.0).scatter(80.0)
            val pts = drawer.bounds.grid(3,3).flatten().map { it.center }


            for (pt in pts) {
                val contour = Circle(pt, Double.uniform(60.0, 120.0)).contour
                val body = contourToBody(contour) {
                    gravity(gravity)
                    linkLengthConstraint {
                        compliance = 1E-3
                    }
                    bodyAreaConstraint {
                        compliance = 1E-2
                    }
                    rectangularBoundsConstraint {
                        bounds = drawer.bounds.offsetEdges(-10.0)
                    }
                }
                sim.bodies.add(body)

                // this is just to get a good screenshot at the initial state
                for (i in 0 until 100) {
                    sim.simulate(1.0 / 60.0, 1)
                }
            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                for (body in sim.bodies) {
                    body.updateBounds()
                    val points = body.nodes.map { it.position }
                    val segments = body.boundaryLinks.map {
                        val it = body.links[it]
                        LineSegment(points[it.source], points[it.target])
                    }
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.fill = ColorRGBa.WHITE
//                    drawer.circles(points, 4.0)
                    drawer.lineSegments(segments)
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.RED
//                    drawer.rectangle(body.bounds)
                }
            }
        }
    }
}