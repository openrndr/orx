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

/**
 * Demonstrates typesetting Arabic script: contextual letter forms, ligatures and right-to-left lines.
 * The text is Article 1 of the Universal Declaration of Human Rights.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val f = loadFace("https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf").withSize(40.0)

            val slugTextDrawer = SlugTextDrawer()
            val box = drawer.bounds.offsetEdges(-40.0)

            val spans = composeText {
                face = f
                styled {
                    sizeInEm = 1.6
                    fill = ColorRGBa.PINK
                    line("المادة الأولى")
                }
                text("يولد جميع الناس ")
                span("أحرارًا") { fill = ColorRGBa.PINK }
                text(" متساوين في ")
                span("الكرامة") { fill = ColorRGBa.PINK }
                text(" و")
                span("الحقوق") { fill = ColorRGBa.PINK }
                text(". وهم قد وهبوا عقلاً وضميرًا وعليهم أن يعامل بعضهم بعضًا بروح الإخاء.")
            }

            slugTextDrawer.style = TextStyle.Defaults.copy(
                lineHeightInEm = 1.4,
                horizontalAlign = 1.0,
                direction = Direction.RIGHT_TO_LEFT,
                verticalAlign = 0.5
            )
            slugTextDrawer.addText(spans, listOf(box))

            extend(Camera2D())
            extend {
                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK.opacify(0.5)
                    drawer.rectangle(box)
                }
                slugTextDrawer.draw(drawer)
            }
        }
    }
}
