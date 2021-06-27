package org.openrndr.panel.tools

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontImageMap
import org.openrndr.draw.isolated
import org.openrndr.draw.writer
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Body
import org.openrndr.panel.elements.Element
import kotlin.math.max

class Tooltip(val parent: Element, val position: Vector2, val message: String) {
    fun draw(drawer: Drawer) {

        val fontUrl = (parent.root() as Body).controlManager.fontManager.resolve("default") ?: error("no font")
        val fontSize = 14.0
        val fontMap = FontImageMap.fromUrl(fontUrl, fontSize)
        val lines = message.split("\n")

        drawer.isolated {
            drawer.fontMap = fontMap

            var maxX = 0.0
            var maxY = 0.0
            writer(drawer) {
                for (line in lines) {
                    newLine()
                    text(line, false)
                    maxX = max(maxX, cursor.x)
                    maxY = cursor.y
                }
                gaplessNewLine()
                maxY = cursor.y
            }

            drawer.translate(position)
            drawer.translate(10.0, 0.0)
            drawer.strokeWeight = 0.5
            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.fill = ColorRGBa.GRAY
            drawer.rectangle(0.0, 0.0, maxX + 20.0, maxY)
            drawer.fill = ColorRGBa.BLACK
            drawer.translate(10.0, 0.0)
            writer(drawer) {
                for (line in lines) {
                    newLine()
                    text(line)
                }
            }
        }
    }
}