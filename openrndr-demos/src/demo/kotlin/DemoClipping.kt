import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.ClipMode
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.drawComposition
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Shape

fun main() = application {
    program {
        val outline = Shape(
            listOf(
                Circle(drawer.bounds.center, 70.0).contour.reversed,
                Circle(drawer.bounds.center, 100.0).contour,
            )
        )

        val radius = outline.bounds.dimensions.length / 2
        val off = outline.bounds.center
        val num = radius.toInt()

        val svg = drawComposition {
            lineSegments(List(num) { segNum ->
                val yNorm = (segNum / (num - 1.0))
                val x = ((segNum % 2) * 2.0 - 1.0) * radius
                val y = (yNorm * 2.0 - 1.0) * radius
                val start = Vector2(-x, y) + off
                val end = Vector2(x, y) + off
                LineSegment(start, end)
            })
            clipMode = ClipMode.INTERSECT
            shape(outline)
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.fill = null
            drawer.shape(outline)
            drawer.composition(svg)
        }
    }
}