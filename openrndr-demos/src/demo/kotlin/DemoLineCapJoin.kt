import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Triangle

/**
 * Test all combinations of line cap and line join by drawing
 * a 3x3 grid of triangles and lines.
 */

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {
            val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 20.0)
            val pointA = Vector2(0.0, 50.0)
            val pointB = Vector2(50.0, -20.0)
            val pointC = Vector2(-50.0, 0.0)
            val triangle = Triangle(pointA, pointB, pointC).contour

            extend {
                drawer.apply {
                    fill = ColorRGBa.GRAY
                    stroke = ColorRGBa.PINK
                    strokeWeight = 8.0
                    fontMap = font
                    LineCap.entries.forEachIndexed { x, cap ->
                        lineCap = cap
                        LineJoin.entries.forEachIndexed { y, join ->
                            lineJoin = join
                            val pos = IntVector2(x - 1, y - 1).vector2 * 180.0
                            isolated {
                                translate(bounds.position(0.46, 0.46) + pos)
                                text("cap: ${cap.name}", -30.5, 80.5)
                                text("join: ${join.name}", -30.5, 100.5)
                                contour(triangle)
                                lineSegment(pointA - pointC, pointB - pointC)
                            }
                        }
                    }
                }
            }
        }
    }
}