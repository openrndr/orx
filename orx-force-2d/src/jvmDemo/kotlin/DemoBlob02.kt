import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.force2d.*
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment

/**
 * This soft body demo simulates the behavior
 * of 9 blobs, initially placed as circles on a
 * 3x3 grid.
 *
 * It uses the same forces and constraints as DemoBlob01:
 * gravity, rectangular-bounds, link-length and body-area
 * constraints.
 *
 * [naiveBroadPhaseCollisionDetector](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/naive-broad-phase-collision-detector.html) and
 * [sapCollisionConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/sap-collision-constraint.html)
 * are required for the blobs to collider with each other.
 *
 * The mouse position is used to control the direction of `gravity`.
 *
 * If the points are initialized at random locations instead
 * of on a grid, blobs can randomly live inside other blobs.
 * Try uncommenting the `scatter` variant, or placing
 * a number of blobs all centered on the screen
 * and see how the simulation evolves.
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
                context = Dispatchers.IO
            }

            //val pts = drawer.bounds.offsetEdges(-50.0).scatter(80.0)
            val pts = drawer.bounds.grid(3, 3).flatten().map { it.center }

            for (pt in pts) {
                val contour = Circle(pt, Double.uniform(60.0, 120.0)).contour
                val body = contourToBody(contour) {
                    gravity(gravity)
                    rectangularBoundsConstraint {
                        bounds = drawer.bounds.offsetEdges(-10.0)
                    }
                    linkLengthConstraint {
                        compliance = 1E-3
                    }
                    bodyAreaConstraint {
                        compliance = 1E-2
                    }
                }
                sim.bodies.add(body)
            }

            if (System.getProperty("takeScreenshot") == "true") {
                extensions.filterIsInstance<SingleScreenshot>().forEach {
                    it.delayFrames = 200
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

//                    drawer.fill = null
//                    drawer.stroke = ColorRGBa.RED
//                    drawer.rectangle(body.bounds)
                }
            }
        }
    }
}