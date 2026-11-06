package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.box
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.fontSize
import org.openrndr.panel.style.px
import org.openrndr.shape.Rectangle

/** how long the pointer rests on an element before its tooltip shows, in seconds */
internal const val tooltipDelay = 0.6

/**
 * The tooltips of elements: the [Element.tooltip] of the innermost element under the pointer shows after the pointer
 * rests on it for [tooltipDelay], next to the pointer, and hides when the pointer moves to another element, presses or
 * scrolls.
 * @param clock the time in seconds, real time by default
 */
internal class Tooltips(private val clock: () -> Double = { System.nanoTime() / 1E9 }) {
    /** the element whose tooltip shows or is about to, where the pointer came to rest on it, and when */
    var element: Element? = null
        private set
    private var position = Vector2.ZERO
    private var since = 0.0

    /** the pointer moved to [position] over [element], the innermost element with a tooltip there */
    fun moved(element: Element?, position: Vector2) {
        if (element !== this.element) {
            this.element = element
            since = clock()
        }
        this.position = position
    }

    /** hides the tooltip until the pointer moves to another element */
    fun hide() {
        element = null
    }

    /** true when the tooltip shows */
    val showing: Boolean get() = element?.tooltip != null && clock() - since >= tooltipDelay

    /** true while a tooltip waits to show, the panel is drawn again until it does */
    val waiting: Boolean get() = element?.tooltip != null && clock() - since < tooltipDelay

    /** draws the tooltip below and right of the pointer, inside [bounds] */
    fun draw(drawer: Drawer, fontManager: FontManager, bounds: Rectangle) {
        if (!showing) return
        val text = element?.tooltip ?: return
        val font = fontManager.font(StyleSheet().apply { fontSize = 13.px })
        val writer = TextWriter(null)
        writer.drawStyle.fontMap = font
        val lines = text.lines()
        val width = lines.maxOf { writer.textWidth(it) } + 16.0
        val lineHeight = font.height * 1.3
        val height = lines.size * lineHeight + 10.0
        // below and right of the pointer, moved into the bounds where it does not fit
        val x = (position.x + 12.0).coerceAtMost(bounds.x + bounds.width - width - 4.0).coerceAtLeast(bounds.x + 4.0)
        val y = (position.y + 18.0).let { if (it + height > bounds.y + bounds.height - 4.0) position.y - height - 8.0 else it }
        drawer.isolated {
            drawer.fill = ColorRGBa.fromHex("#1f1f1f").opacify(0.95)
            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.strokeWeight = 1.0
            drawer.box(Rectangle(x, y, width, height), 4.0)
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE.shade(0.9)
            drawer.stroke = null
            for ((i, line) in lines.withIndex()) {
                drawer.text(line, x + 8.0, y + 5.0 + font.ascenderLength + i * lineHeight)
            }
        }
    }
}
