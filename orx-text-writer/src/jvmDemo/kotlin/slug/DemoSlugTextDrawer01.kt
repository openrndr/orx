package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val pface = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(16.0)


            val slugTextDrawer = SlugTextDrawer()

            val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"

            val box = Rectangle(10.0, 360.0, 350.0, 360.0)


            slugTextDrawer.addText(text,
                box, TextStyle(pface, horizontalAlign = 0.5, verticalAlign = 0.5))

            slugTextDrawer.addText(text,
                box.movedBy(Vector2(360.0, 0.0)), style = TextStyle(pface, justify = true, lineHeightInEm = 1.2)
            )
            extend(Camera2D())
            extend {
                drawer.translate(drawer.bounds.center)
                drawer.scale(1.0 + 2.0 * mouse.position.y / height)
                drawer.translate(-drawer.bounds.center)


                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK
                    drawer.rectangle(box)
                }

                slugTextDrawer.draw(drawer)
            }
        }
    }
}