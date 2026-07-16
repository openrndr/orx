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
import org.openrndr.extra.force2d.nodeCollisionConstraint
import org.openrndr.extra.force2d.pointsToBody
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.force2d.sapCollisionConstraint
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
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
            }

            val pts = drawer.bounds.scatter(15.0)
            run {
                val body = pointsToBody(pts, radius = 10.0) {
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
            }

            run {
                val contour = Circle(drawer.bounds.center, Double.uniform(60.0, 120.0)).contour
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
            }
            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate( 1.0 / 60.0, 10)
                }
                val circles = sim.bodies[0].nodes.map { it.position }
                drawer.circles(circles, 10.0)

                for (body in sim.bodies) {
                    body.updateBounds()
                    val points = body.nodes.map { it.position }
                    val segments = body.boundaryLinks.map {
                        val it = body.links[it]
                        LineSegment(points[it.source], points[it.target])
                    }
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.fill = ColorRGBa.WHITE
                    drawer.lineSegments(segments)
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.RED
                }
            }
        }
    }
}