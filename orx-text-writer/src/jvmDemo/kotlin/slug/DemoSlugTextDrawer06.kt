package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.composeText
import org.openrndr.shape.Rectangle

/**
 * Demonstrates bidirectional text: a left-to-right paragraph with embedded Arabic words and a
 * right-to-left paragraph with embedded English words and numbers.
 *
 * Every embedded run keeps its own reading order, multi-word runs are not reversed word by word.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val latin = loadFace("https://raw.githubusercontent.com/d-sargent/platypi/main/fonts/ttf/Platypi-Regular.ttf").withSize(30.0)
            val arabic = loadFace("https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf").withSize(36.0)

            val slugTextDrawer = SlugTextDrawer()

            val top = Rectangle(40.0, 40.0, 640.0, 300.0)
            val bottom = Rectangle(40.0, 380.0, 640.0, 300.0)

            val leftToRight = composeText {
                face = latin
                direction = Direction.LEFT_TO_RIGHT
                text("This English paragraph contains the Arabic words ")
                span("مرحبا بالعالم") { face = arabic; fill = ColorRGBa.PINK }
                text(" (hello world) and ")
                span("المصدر المفتوح") { face = arabic; fill = ColorRGBa.PINK }
                text(" (open source). Each embedded run reads from right to left, while the paragraph " +
                        "reads from left to right.")
            }

            val rightToLeft = composeText {
                face = arabic
                direction = Direction.RIGHT_TO_LEFT
                horizontalAlign = 1.0
                text("تحتوي هذه الفقرة العربية على كلمات إنجليزية مثل ")
                span("open source software") { face = latin; fill = ColorRGBa.PINK }
                text(" و ")
                span("creative coding") { face = latin; fill = ColorRGBa.PINK }
                text("، وأرقام مثل ")
                span("2026") { face = latin; fill = ColorRGBa.PINK }
                text(" و")
                span("٢٠٢٦") { fill = ColorRGBa.PINK }
                text(". تُقرأ الفقرة من اليمين إلى اليسار.")
            }

            slugTextDrawer.style = TextStyle.Defaults.copy(lineHeightInEm = 1.4, verticalAlign = 0.5)
            slugTextDrawer.addText(leftToRight, listOf(top))
            slugTextDrawer.addText(rightToLeft, listOf(bottom))

            extend(Camera2D())
            extend {
                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK.opacify(0.5)
                    drawer.rectangles(listOf(top, bottom))
                }
                slugTextDrawer.draw(drawer)
            }
        }
    }
}
