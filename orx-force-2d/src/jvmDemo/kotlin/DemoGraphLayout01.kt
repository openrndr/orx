import kotlinx.coroutines.runBlocking
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.force2d.*
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.extra.noise.scatter
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment

/**
 * Demonstrates a custom simulation by manually creating nodes, links between those nodes,
 * and then a body containing them. The nodes are initially scattered around the drawer
 * bounds, and links are created for each node to the two nearest ones.
 *
 * The simulation enforces a link-length constraint, a rectangular-bounds constraint for
 * the borders of the window, and a node-repulse force.
 *
 * After a few seconds, the simulation falls into a semi-stable state, but this
 * is not a given and very much depends on the simulation values.
 *
 * Not how instead of adding bodies one by one to the `ForceSimulation` they are passed
 * all at one in the constructor.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val pts = drawer.bounds.scatter(20.0)
            val index = pts.mapIndexed { index, vector2 -> vector2 to index }.toMap()
            val kd = pts.kdTree()

            val nodes = pts.map {
                Node(it, it, Vector2.ZERO, radius = 4.0)
            }

            val links = pts.flatMapIndexed { source, it ->
                kd.findKNearest(it, 2).map {
                    val idx = index.getValue(it)
                    Link(source, idx)
                }
            }

            val body = Body(nodes, links = links).apply {
                linkLengthConstraint {
                    compliance = 1E-1
                }
                nodeRepulseForce {
                    searchRadius = 100.0
                    strength = 10.0
                }
                rectangularBoundsConstraint {
                    bounds = drawer.bounds.offsetEdges(-10.0)
                }
            }

            val sim = ForceSimulation(mutableListOf(body))

            extend {
                runBlocking {
                    sim.simulate(1.0 / 60.0, 10)
                }

                drawer.stroke = ColorRGBa.PINK
                drawer.lineSegments(body.links.map {
                    LineSegment(body.nodes[it.source].position, body.nodes[it.target].position)
                })

                drawer.circles(body.nodes.map { it.position }, 4.0)
            }
        }
    }
}