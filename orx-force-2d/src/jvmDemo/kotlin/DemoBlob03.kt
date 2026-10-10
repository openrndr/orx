import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.extra.color.presets.PERU
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.force2d.*
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour

/**
 * A soft body simulation that begins with four stars placed
 * in a 2x2 grid and evolves into hard to predict outcomes
 * based on the parameters and their relation.
 *
 * On top of a Gravity force, this one features a
 * [NodeRepulseForce](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-node-repulse-force/index.html)
 * and a
 * [NodeRepulseInterbodyForce](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/node-repulse-interbody-force.html).
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

            drawer.bounds.grid(2, 2).flatten().forEach { cell ->
                //val contour = Rectangle.fromCenter(drawer.bounds.center, 400.0).contour
                val contour = regularStar(12, 50.0, 95.0, cell.center)
                val body = contourToBody(contour, density = 5.0, linkNeighbors = 3) {
                    gravity(gravity)
                    rectangularBoundsConstraint {
                        bounds = drawer.bounds.offsetEdges(-10.0)
                    }
                    linkLengthConstraint {
                        compliance = 1E-4
                        iterations = 2
                    }
                    bodyAreaConstraint {
                        compliance = 1E-4
                        iterations = 3
                    }
                    nodeRepulseForce {
                        searchRadius = 300.0
                        strength = 100.0
                    }
                }
                sim.bodies.add(body)

            }
            sim.nodeRepulseInterbodyForce {
                searchRadius = 20.0
                strength = 100.0
            }

            extend {
                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.WHITE)
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }
                drawer.drawStyle.blendMode = BlendMode.MULTIPLY
                for ((index, body) in sim.bodies.withIndex()) {
                    body.updateBounds()
                    val points = body.nodes.map { it.position }
                    val segments = body.boundaryLinks.map {
                        val it = body.links[it]
                        Segment2D(points[it.source], points[it.target])
                    }
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.PERU.shiftHue<OKHSV>(90.0 * -index).opacify(0.8)

                    val c = ShapeContour.fromSegments(segments, closed = true)
                    drawer.contour(c)
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.RED
                }
            }
        }
    }
}