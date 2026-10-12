package medialaxis

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.medialaxis.medialAxis
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.triangulation.constrainedDelaunayTriangulation
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.*

/**
 * Medial axes of a few shapes. Gray: the raw approximation given by the internal edges of the constrained
 * Voronoi diagram of the sampled boundary. Colored: the fitted medial axis branches, one color per branch.
 */
fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val cells = listOf(Vector2(200.0, 200.0), Vector2(600.0, 200.0), Vector2(200.0, 600.0), Vector2(600.0, 600.0)).map { Rectangle.fromCenter(it, 380.0, 380.0) }

            val shapes = listOf(
                Rectangle.fromCenter(cells[0].center, 300.0, 150.0).shape,
                Ellipse(cells[1].center, 160.0, 90.0).shape,
                Shape(listOf(Circle(cells[2].center, 150.0).contour, Circle(cells[2].center + Vector2(30.0, 10.0), 60.0).contour.reversed)),
                ShapeContour.fromPoints(List(9) { i ->
                    val r = if (i % 2 == 0) 160.0 else 70.0
                    cells[3].center + Polar(i * 360.0 / 9.0, r).cartesian
                }, closed = true).hobbyCurve().shape
            )
            val sd = 3.0
            val axes = shapes.map { it.medialAxis(samplingDistance = sd) }
            for ((i, axis) in axes.withIndex()) println("shape $i: ${axis.branches.size} branches, ${axis.nodes.size} nodes")

            extend {
                drawer.clear(ColorRGBa.WHITE)
                for ((i, shape) in shapes.withIndex()) {
                    drawer.fill = ColorRGBa.PINK.opacify(0.2)
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.strokeWeight = 1.0
                    drawer.shape(shape)

//                    val (vertices, edges) = raw[i]
                    drawer.stroke = ColorRGBa.DARK_GRAY.opacify(0.5)
//                    drawer.lineSegments(edges.map { (a, b) -> LineSegment(vertices[a], vertices[b]) })

                    drawer.strokeWeight = 2.0
                    drawer.fill = null
                    for ((j, branch) in axes[i].branches.withIndex()) {
                        drawer.stroke = ColorRGBa.fromHex(listOf(0xe6194b, 0x3cb44b, 0x4363d8, 0xf58231, 0x911eb4, 0x008080)[j % 6])
                        drawer.contour(branch.contour)

                        val rect = branch.contour.rectified()
                        val steps = (rect.contour.length / 10.0).toInt()
                        for (k in 0 until steps) {
                            val position = rect.position(k/steps.toDouble())
                            val distance = shape.nearest(position).position.distanceTo(position)

                            drawer.circle(position, distance)
                        }
                    }
                    drawer.fill = ColorRGBa.BLACK
                    drawer.stroke = null
                    drawer.circles(axes[i].nodes, 3.0)
                }
            }
        }
    }
}
