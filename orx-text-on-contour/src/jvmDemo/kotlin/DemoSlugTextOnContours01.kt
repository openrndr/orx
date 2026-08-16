import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.slug.TextSpan
import org.openrndr.draw.slug.TextStyle
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
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf", 32.0, 1.0)

        val slugMap = SlugMap(
            colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
            colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
        )
        slugMap.bands.filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)

        val slugGlyphMap = SlugGlyphMap(slugMap)
        val slugDrawer = SlugDrawer()

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
                drawer, slugDrawer, slugGlyphMap,
                TextStyle.Defaults.copy(face = face, justify = true),
                spans, contours, seconds * 0.02
            )
        }
    }
}
