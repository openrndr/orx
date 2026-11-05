import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates markers, drawn at the vertices of paths:
 *
 * - arrowheads sized in stroke widths, with orient="auto-start-reverse" so the arrow at the start points backwards
 * - dots in user space units, set with the CSS `marker` shorthand in a style sheet
 * - ticks that are turned along the bisectors of a polygon's corners, and diamonds at a fixed angle
 * - markers whose content overflows their viewport and is clipped, on a path with smooth quadratic segments
 *
 * The zigzag and the curve are changed every frame; their markers follow the vertices and directions.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/markers.svg")
        val zigzag = composition.findShapes().first { it.id == "zigzag" }
        val curve = composition.findShapes().first { it.id == "curve" }

        extend {
            zigzag.shape = ShapeContour.fromPoints((0..4).map {
                Vector2(360.0 + it * 80.0, 100.0 + (if (it % 2 == 0) -40.0 else 40.0) * cos(seconds + it * 0.7))
            }, closed = false).shape
            curve.shape = contour {
                moveTo(40.0, 160.0)
                curveTo(Vector2(120.0, 80.0 + 60.0 * sin(seconds)), Vector2(220.0, 240.0), Vector2(300.0, 140.0 + 40.0 * sin(seconds * 0.7)))
            }.shape
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
