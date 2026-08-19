
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.ForceSimulation
import org.openrndr.extra.force2d.GravityForce
import org.openrndr.extra.force2d.NodeRepulseForce
import org.openrndr.extra.force2d.bodyAreaConstraint
import org.openrndr.extra.force2d.contourToBody
import org.openrndr.extra.force2d.gravity
import org.openrndr.extra.force2d.linkLengthConstraint
import org.openrndr.extra.force2d.nodeRepulseForce
import org.openrndr.extra.force2d.nodeRepulseInterbodyForce
import org.openrndr.extra.force2d.rectangularBoundsConstraint
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.LineSegment

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

//            extend(ScreenRecorder()) {
//                frameRate = 60.0
//                contentScale = 1.0
//                frameClock = false
//            }

            val sim = ForceSimulation()

            val gravity = GravityForce()

//            sim.apply {
//                naiveBroadPhaseCollisionDetector()
//                sapCollisionConstraint()
//            }

            val pts = drawer.bounds.scatter(15.0)

            drawer.bounds.grid(2, 2).flatten().forEach { cell ->



                //val contour = Rectangle.fromCenter(drawer.bounds.center, 400.0).contour
                val contour = regularStar(10, 50.0, 200.0, cell.center)
                val body = contourToBody(contour, density = 10.0, linkNeighbors = 5) {
                    gravity(gravity)

                    linkLengthConstraint {
                        compliance = 1E-4
                        iterations = 2
                    }
                    bodyAreaConstraint {
                        compliance = 0.0
                        iterations = 3
                    }
                    nodeRepulseForce {
                        searchRadius = 45.0
                        strength = .0
                    }
                    rectangularBoundsConstraint {
                        bounds = drawer.bounds.offsetEdges(-10.0)
                    }
                }
                sim.bodies.add(body)

            }
            sim.nodeRepulseInterbodyForce {
                searchRadius = 30.0
                strength = 1000.0
            }

            sim.context = Dispatchers.IO


            extend {

                gravity.gravity = (mouse.position - drawer.bounds.center) * 1.0

                drawer.clear(ColorRGBa.PINK)
                runBlocking {
                    sim.simulate( 1.0 / 60.0, 10)
                }

                (sim.bodies[0].forces.getOrNull(1) as? NodeRepulseForce)?.strength = 10.0 //* mouse.position.y / height.toDouble()
                (sim.bodies[1].forces.getOrNull(1) as? NodeRepulseForce)?.strength = 10.0 //* mouse.position.y / height.toDouble()

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

                drawer.defaults()


            }
        }

    }
}