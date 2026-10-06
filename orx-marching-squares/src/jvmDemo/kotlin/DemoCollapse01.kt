import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.marchingsquares.QuadTree
import org.openrndr.extra.marchingsquares.buildQuadTree
import org.openrndr.extra.marchingsquares.findDualGraph
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Demonstrates how, given a user-defined SDF
 * function that takes a [Vector2] as input and outputs a signed Double,
 * [QuadTree.buildQuadTree](https://orx.openrndr.org/orx-marching-squares/org.openrndr.extra.marchingsquares/build-quad-tree.html)
 * is used to build a quad tree out of that SDF function, and
 * [QuadTree.findDualGraph](https://orx.openrndr.org/orx-marching-squares/org.openrndr.extra.marchingsquares/find-dual-graph.html),
 * converts the quad tree into a list of vertices and a list of vertex indices defining polygonal faces.
 *
 * The program uses these tools to render an animated SDF as a segment-based polygonal mesh.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.clear(ColorRGBa.WHITE)

                val shape = { p: Vector2 ->
                    val q = Vector2(
                        p.x + cos(seconds + p.y * 0.04) * 120.0,
                        p.y + sin(seconds + p.x * 0.04) * 120.0
                    ); (q - drawer.bounds.center).length - 250.0
                }
                val side = max(width, height).toDouble()
                val area = Rectangle.fromCenter(drawer.bounds.center, side, side)
                val maxDepth = 7

                val rootQuadTree = QuadTree.buildQuadTree(shape, area, maxDepth)
                val dualGraph = rootQuadTree.findDualGraph()

                drawer.fill = null

                val faces = dualGraph.second
                val vertices = dualGraph.first

                val segments = mutableListOf<LineSegment>()
                for (face in faces) {
                    for (i in face.indices) {
                        val vertex = vertices[face[i]]
                        val nextVertex = vertices[face[(i + 1) % face.size]]
                        segments.add(LineSegment(vertex, nextVertex))
                    }
                }
                drawer.lineSegments(segments)
            }
        }
    }
}