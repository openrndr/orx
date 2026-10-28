import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.textoncontour.slugTextOnContours
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.composeText
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.sin

/**
 * Demo Functionality includes:
 * - Typesetting right-to-left Arabic text (Article 1 of the Universal Declaration of Human Rights)
 *   such that it overflows from one wavy contour to the next.
 * - Using [Direction.RIGHT_TO_LEFT] to lay out the words of every line from right to left.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val face = loadFace("https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf").withSize(36.0)

        val slugMap = SlugMap(
            colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
            colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
        )

        val slugGlyphMap = SlugGlyphMap(slugMap)
        val slugDrawer = SlugDrawer()

        fun contours(phase: Double) = (0 until 10).map { line ->
            ShapeContour.fromPoints(List(100) { i ->
                val x = 40.0 + i * 640.0 / 99
                Vector2(x, 80.0 + line * 64.0 + sin(x * 0.01 + line * 0.4 + phase) * 20.0)
            }, closed = false).rectified()
        }

        val spans = composeText {
            repeat(4) {
                text("يولد جميع الناس ")
                span("أحرارًا") { fill = ColorRGBa.PINK }
                text(" متساوين في ")
                span("الكرامة") { fill = ColorRGBa.PINK }
                text(" و")
                span("الحقوق") { fill = ColorRGBa.PINK }
                text(". وهم قد وهبوا عقلاً وضميرًا وعليهم أن يعامل بعضهم بعضًا بروح الإخاء. ")
            }
        }

        extend {
            slugTextOnContours(
                drawer, slugDrawer, slugGlyphMap,
                TextStyle.Defaults.copy(face = face, justify = true, direction = Direction.RIGHT_TO_LEFT),
                spans, contours(seconds)
            )
        }
    }
}
