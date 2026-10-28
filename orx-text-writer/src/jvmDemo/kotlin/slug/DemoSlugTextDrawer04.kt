package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.StrokeMode
import org.openrndr.extra.textwriter.composeText
import org.openrndr.extra.textwriter.TextStyle
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val f = loadFace("https://raw.githubusercontent.com/d-sargent/platypi/main/fonts/ttf/Platypi-Regular.ttf").withSize(64.0)

            val slugTextDrawer = SlugTextDrawer()

            val text = "OPENRNDR"
            val box = drawer.bounds.offsetEdges(-20.0)

            extend(Camera2D())
            extend {
                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK
                    drawer.rectangle(box)
                }

                val sw = cos(seconds) * 2.0 + 2.0

                val spans = composeText {
                    face = f
                    fill = ColorRGBa.GRAY
                    line(text)
                    styled {
                        strokeWeight = sw
                        stroke = ColorRGBa.WHITE
                        span("$text\n") { strokeMode = StrokeMode.CENTER }
                        span("$text\n") { strokeMode = StrokeMode.INNER }
                        span("$text\n") { strokeMode = StrokeMode.OUTER }
                        span(text) {
                            fill = ColorRGBa.GRAY.shade(0.5)
                            strokeMode = StrokeMode.ERODE
                        }
                    }
                }

                slugTextDrawer.clear()
                slugTextDrawer.style = TextStyle.Defaults.copy(verticalAlign = 0.5, horizontalAlign = 0.5)
                slugTextDrawer.addText(spans, listOf(box))

                slugTextDrawer.draw(drawer)
            }
        }
    }
}