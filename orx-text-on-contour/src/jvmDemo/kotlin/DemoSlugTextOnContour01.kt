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
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.textoncontour.slugTextOnContour
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.Circle
import kotlin.math.PI
import kotlin.math.cos

/**
 * Demo Functionality includes:
 * - Loading and applying a specific font (`IBMPlexMono-Regular`) with a size of 32.0.
 * - Creating a circular contour at the center of the screen with a radius of 200.0.
 * - Rendering text along the rectified circle's contour.
 * - Offsetting text positions, enabling repeated text rendering along the same contour.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {

        extend(ScreenRecorder()) {
            maximumDuration = 10.0
            frameRate = 60.0
        }
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf", 32.0, 1.0)

        val slugMap = SlugMap(
            colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
            colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
        )

        slugMap.bands.filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)

        val slugGlyphMap = SlugGlyphMap(slugMap)
        val slugDrawer = SlugDrawer()
        extend(Camera2D())
        extend {
            val c = Circle(drawer.bounds.center, 200.0).contour.rectified()

            var spans = listOf(TextSpan("The wheels of the bus go round and round.", TextStyle(textWidthFactor = 1.0, sizeInEm = 1.3)))

            spans = spans.flatMap { it.text.mapIndexed { index, c ->  TextSpan(c.toString(), it.style!!.copy(textWidthFactor = cos(seconds * PI * 2.0 * 0.25 + index*0.1) * 0.4 + 0.6, sizeInEm  = cos(seconds * PI * 4.0 * 0.5 + index*0.0465) * 0.4 + 1.4)) } }

            slugTextOnContour(drawer, slugDrawer, slugGlyphMap, TextStyle.Defaults.copy(face = face),
                spans, c, seconds * 0.5)

        }
    }
}