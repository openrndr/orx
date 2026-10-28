package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextComposer
import org.openrndr.extra.textwriter.composeText
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val f2 = loadFace("https://raw.githubusercontent.com/d-sargent/platypi/main/fonts/ttf/Platypi-Regular.ttf").withSize(32.0)

            val scripts = f2.supportedScripts()
            for (script in scripts) {
                println(script)
                println(f2.substitutionFeatures(script))
            }
            val slugTextDrawer = SlugTextDrawer()

            val box = Rectangle(10.0, 10.0, 350.0, 700.0)
            val box2 = box.movedBy(Vector2(360.0, 0.0))

            val spans = composeText {
                face = f2
                fill = ColorRGBa.WHITE
                val subscript: TextComposer.() -> Unit = {
                    sizeInEm = 0.5
                    baselineShiftInEm = -0.25
                }
                for (i in 0 until 100) {
                    text("NO")
                    span("x", block = subscript)
                    text(" CO")
                    span("2", block = subscript)
                    text(" H")
                    span("2", block = subscript)
                    text("O ")
                }
            }
            slugTextDrawer.addText(spans, listOf(box, box2))

            extend(Camera2D())
            extend {
                slugTextDrawer.draw(drawer)
                Driver.instance.finish()
                val now = System.currentTimeMillis()
            }
        }
    }
}