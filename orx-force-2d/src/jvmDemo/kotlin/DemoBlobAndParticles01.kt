import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.*
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment

/**
 * Demonstration of how to include two types of bodies in the same simulation:
 * a blob and circular particles. It could be seen as a combination of one of
 * DemoBlob02 and DemoParticles01.
 *
 * Both bodies have the same rectangular-bounds constraint to make them collide
 * with the window borders and are affected by the same mouse-controlled gravity.
 *
 * A helper function to draw the outline of a blob is provided.
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

            fun drawBlobOutline(blob: Body) {
                val points = blob.nodes.map { it.position }
                val segments = blob.boundaryLinks.map {
                    val link = blob.links[it]
                    LineSegment(points[link.source], points[link.target])
                }
                drawer.lineSegments(segments)
            }

            // For a better README.md screenshot
            gravity.gravity = Vector2(-450.0)
            repeat(35) { sim.simulate(1.0 / 60.0, 10) }

            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }

                sim.bodies.forEach { body -> body.updateBounds() }

                val circles = sim.bodies[0].nodes.map { it.position }
                drawer.circles(circles, 10.0)

                drawer.stroke = ColorRGBa.BLACK
                drawer.fill = ColorRGBa.WHITE
                drawBlobOutline(sim.bodies[1])
            }
        }
    }
}