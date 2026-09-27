import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.marchingsquares.QuadTree
import org.openrndr.extra.marchingsquares.buildQuadTree
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.marchingsquares.findContoursAdaptive
import org.openrndr.extra.marchingsquares.findContoursAdaptiveMarchingSquares
import org.openrndr.extra.marchingsquares.findDualGraph
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import java.util.concurrent.ForkJoinTask.adapt
import javax.swing.text.Segment
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

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
                val area = drawer.bounds
                val maxDepth = 7
                val side = max(width, height).toDouble()
                val root = QuadTree.buildQuadTree(shape, Rectangle.fromCenter(area.center, side, side), maxDepth)
                val dualGraph = root.findDualGraph()

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