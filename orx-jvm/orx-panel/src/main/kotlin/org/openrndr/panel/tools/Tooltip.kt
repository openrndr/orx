package org.openrndr.panel.tools

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Element
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.fontSize
import org.openrndr.panel.style.px
import org.openrndr.shape.Rectangle

class Tooltip(val parent: Element, val position: Vector2, val message: String) {
    private val style = StyleSheet().apply { fontSize = 14.px }

    /** the area the tooltip covers, relative to the position of [parent], null when it is not in a control manager */
    val area: Rectangle?
        get() {
            val text = parent.panelText ?: return null
            val face = text.face(style)
            val lineHeight = face.ascent - face.descent + face.lineGap
            val lines = message.split("\n")
            val width = lines.maxOf { text.width(style, it) }
            return Rectangle(position.x + 10.0, position.y, width + 20.0, (lines.size + 1) * lineHeight)
        }

    fun draw(drawer: Drawer) {
        val text = parent.panelText ?: return
        val face = text.face(style)
        val lineHeight = face.ascent - face.descent + face.lineGap
        val lines = message.split("\n")
        val width = lines.maxOf { text.width(style, it) }
        val height = (lines.size + 1) * lineHeight

        drawer.isolated {
            drawer.translate(position)
            drawer.translate(10.0, 0.0)
            drawer.strokeWeight = 0.5
            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.fill = ColorRGBa.GRAY
            drawer.rectangle(0.0, 0.0, width + 20.0, height)
            for ((i, line) in lines.withIndex()) {
                text.drawLine(drawer, this@Tooltip, "line $i", style, line, 10.0, (i + 1) * lineHeight, ColorRGBa.BLACK)
            }
        }
    }
}
