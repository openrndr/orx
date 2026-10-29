import org.openrndr.application
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.textoncontour.slugTextOnContours
import org.openrndr.shape.Circle

/**
 * Demo Functionality includes:
 * - Loading and applying a specific font (`IBMPlexMono-Regular`).
 * - Creating a series of concentric circular contours.
 * - Typesetting a text such that it overflows from one contour to the next.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(32.0)

        val boundaryDrawer = drawer.boundaryDrawer
        val glyphMap = BoundaryGlyphMap(boundaryDrawer.createMap())

        val contours = (0 until 6).map {
            Circle(drawer.bounds.center, 330.0 - it * 40.0).contour.rectified()
        }

        val spans = listOf(
            TextSpan(
                "The wheels of the bus go round and round, round and round, round and round. " +
                        "The wheels of the bus go round and round, all through the town.",
                TextStyle(sizeInEm = 1.0, textWidthFactor = 1.0)
            )
        )

        extend {
            slugTextOnContours(
                drawer, boundaryDrawer, glyphMap,
                TextStyle.Defaults.copy(face = face, justify = true),
                spans, contours, seconds * 0.02
            )
        }
    }
}
