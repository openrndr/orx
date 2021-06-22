import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour

/**
 * a simple demo that tests line joins
 *
 * This was made to assist in resolving https://github.com/openrndr/openrndr/issues/162
 */

fun arc(start: Vector2, end: Vector2, radius: Double): ShapeContour {
    return contour {
        moveTo(start)
        arcTo(radius, radius, 0.0, false, false, end)
    }
}

suspend fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        val center = Vector2(width / 2.0, height / 2.0)
        val extra = Vector2(75.0, 75.0)

        extend {
            drawer.clear(ColorRGBa.PINK)

            drawer.lineJoin = LineJoin.BEVEL

            drawer.strokeWeight = 40.0
            drawer.contour(arc(center - extra, center - extra - extra, 75.0))
            drawer.contour(arc(center, center + extra, 75.0 / 2.0))
            drawer.contour(arc(center + extra + extra, center + extra, 75.0 / 2.0))
        }
    }
}