import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.extra.color.presets.PERU
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.NodeRepulseForceBVH
import org.openrndr.extra.force2d.bodyAreaConstraint
import org.openrndr.extra.force2d.contourToBody
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.linkLengthConstraint
import org.openrndr.extra.force2d.nodeRepulseForceBVH
import org.openrndr.extra.force2d.nodeRepulseInterbodyForceBVH
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.math.map
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos

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
                val contour = regularStar(12, 90.0, 195.0, cell.center)
                val body = contourToBody(contour, density = 5.0, linkNeighbors = 1) {
                    gravity(gravity)

                    rectangularBoundsConstraint {
                        bounds = drawer.bounds.offsetEdges(-10.0)
                    }

                    linkLengthConstraint {
                        compliance = 1E-2
                        iterations = 1
                    }
                    bodyAreaConstraint {
                        compliance = 0.0
                        iterations = 1
                    }
                    nodeRepulseForceBVH {
                        searchRadius = 30.0
                        strength = 100.0
                    }
                }
                sim.bodies.add(body)
            }
            sim.nodeRepulseInterbodyForceBVH {
                searchRadius = 20.0
                strength = 100.0
            }

            for (i in 0 until 100) {
                sim.simulate(1.0 / 60.0, 10)
            }

            sim.context = Dispatchers.IO
            extend {
                drawer.clear(ColorRGBa.WHITE)
                (sim.bodies[0].forces.getOrNull(1) as? NodeRepulseForceBVH)?.searchRadius =
                    cos(seconds * Math.PI).map(-1.0..1.0, 10.0..40.0)
                (sim.bodies[1].forces.getOrNull(1) as? NodeRepulseForceBVH)?.searchRadius =
                    cos(seconds * Math.PI + PI / 4).map(-1.0..1.0, 10.0..40.0)
                (sim.bodies[2].forces.getOrNull(1) as? NodeRepulseForceBVH)?.searchRadius =
                    cos(seconds * Math.PI + PI / 2.0).map(-1.0..1.0, 10.0..40.0)
                (sim.bodies[3].forces.getOrNull(1) as? NodeRepulseForceBVH)?.searchRadius =
                    cos(seconds * Math.PI + 3 * PI / 4.0).map(-1.0..1.0, 10.0..40.0)

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
                }
            }
        }
    }
}