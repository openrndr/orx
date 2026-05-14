import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.extra.rtree.Polygon2D
import org.openrndr.extra.rtree.RtreePolygon2D
import org.openrndr.extra.shapes.primitives.regularStar

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val polygons = mutableListOf<Polygon2D>()
            for (i in 0 until 5) {
                val s = regularStar(5, 100.0, 150.0, center = drawer.bounds.uniform())
                val points = s.segments.map { it.start }
                polygons.add(points)
            }
            val rtree = RtreePolygon2D()

            for (polygon in polygons) {
                rtree.insert(polygon)
            }
            extend {
                drawer.clear(ColorRGBa.PINK)
                polygons.forEach { polygon ->
                    drawer.lineLoop(polygon)
                }
                val n = rtree.findKNearest(mouse.position, 1)
                drawer.stroke = ColorRGBa.RED
                if (n.isNotEmpty())
                drawer.lineLoop(n.first())

                val (fp, fr) = rtree.findNearestOpenSpace(mouse.position)
                drawer.fill = null
                drawer.rectangle(fr)
                drawer.circle(fp, 10.0)

            }
        }
    }
}