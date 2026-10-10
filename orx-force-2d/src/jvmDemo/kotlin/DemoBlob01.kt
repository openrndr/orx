import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.BodyAreaConstraint
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.RectangularBoundsConstraint
import org.openrndr.extra.force2d.bodyAreaConstraint
import org.openrndr.extra.force2d.contourToBody
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.linkLengthConstraint
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

/**
 * A demonstration with two soft body blobs.
 *
 * The horizontal mouse position is used to divide the space in two adjacent
 * rectangles, each controlling the dimensions of a
 * [rectangularBoundsConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-rectangular-bounds-constraint/index.html)
 *
 * Each blob lives inside those constraint rectangles.
 *
 * A [bodyAreaConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-body-area-constraint/index.html)
 * "inflates" / "deflates" the blob to the specified `restArea`. In this example,
 * the vertical mouse position controls how much of the available constraint rectangle area
 * is used: 100% at the bottom and 0% at the bottom.
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

            val grid = drawer.bounds.grid(2, 1).flatten()

            for (cell in grid) {
                val contour = Circle(cell.center, cell.width / 2.0).contour
                val body = contourToBody(contour) {
                    gravity(gravity)
                    rectangularBoundsConstraint {
                        bounds = cell
                        bounce = 0.0
                        compliance = 0.0
                        iterations = 10
                    }
                    linkLengthConstraint {
                        compliance = 1E-4
                        iterations = 10
                    }
                    bodyAreaConstraint {
                        compliance = 0.0
                    }
                }
                sim.bodies.add(body)
            }

            extend {
                val leftArea = Rectangle(0.0, 0.0, mouse.position.x, height.toDouble()).offsetEdges(-10.0)
                val rightArea = Rectangle(mouse.position.x, 0.0, width - mouse.position.x, height.toDouble()).offsetEdges(-10.0)
                (sim.bodies[0].constraints[0] as RectangularBoundsConstraint).bounds = leftArea
                (sim.bodies[1].constraints[0] as RectangularBoundsConstraint).bounds = rightArea

                val f = mouse.position.y / height.toDouble()
                (sim.bodies[0].constraints[2] as BodyAreaConstraint).restArea = leftArea.area * f
                (sim.bodies[1].constraints[2] as BodyAreaConstraint).restArea = rightArea.area * f

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }
                gravity.gravity = Vector2(0.0, 10.0)

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
                }
            }
        }
    }
}