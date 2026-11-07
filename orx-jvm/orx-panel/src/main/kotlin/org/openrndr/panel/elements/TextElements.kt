package org.openrndr.panel.elements

import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontMap
import org.openrndr.draw.StrokeAlignment
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.reflect.KMutableProperty0

class TextNode(text: String) : Element(ElementType("text")) {
    /** the text, a change lays out and draws again */
    var text: String = text
        set(value) {
            if (value != field) {
                field = value
                requestRedraw()
                requestLayout()
            }
        }

    override fun draw(drawer: Drawer) {
        val color = (computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE
        panelText?.drawInBox(
            drawer, this, "text", computedStyle, text, layout.contentBoundsAtOrigin,
            computedStyle.computedTextHorizontalAlign, computedStyle.computedTextVerticalAlign, color, wrap = true
        )
    }

    private val text0: org.openrndr.panel.PanelText get() = panelText ?: error("no control manager")

    /**
     * The width of the widest line of the text when it is not wrapped.
     */
    fun textWidth(): Double = text.lines().maxOf { text0.width(computedStyle, it) }

    /**
     * The size of the text when it is wrapped at [width], from the ascent of the first line to the descent of the
     * last line, as it is drawn.
     */
    fun sizeHint(width: Double = layout.screenWidth): Rectangle {
        val height = text0.paragraph(computedStyle, text, width, computedStyle.computedTextHorizontalAlign).height
        return Rectangle(layout.screenX, layout.screenY, width, height)
    }

    override fun toString(): String {
        return "TextNode(id='$id',text='$text')"
    }
}

class H1 : TextElement(ElementType("h1"))
class H2 : TextElement(ElementType("h2"))
class H3 : TextElement(ElementType("h3"))
class H4 : TextElement(ElementType("h4"))
class H5 : TextElement(ElementType("h5"))

class P : TextElement(ElementType("p"))
class Label : TextElement(ElementType("label"))

abstract class TextElement(et: ElementType) : Element(et) {
    /** draws the background and the border of the box when they are set, the text is drawn by its text nodes */
    override fun draw(drawer: Drawer) {
        val background = computedStyle.effectiveBackground ?: ColorRGBa.TRANSPARENT
        val border = computedStyle.effectiveBorderColor ?: ColorRGBa.TRANSPARENT
        if (background.alpha <= 0.0 && (border.alpha <= 0.0 || layout.borderWidth <= 0.0)) return
        drawer.isolated {
            drawer.fill = background
            drawer.stroke = border
            drawer.strokeWeight = layout.borderWidth
            drawer.strokeAlignment = StrokeAlignment.INSIDE
            drawer.box(layout.boundsAtOrigin, cornerRadius)
        }
    }

    fun text(text: String) {
        append(TextNode(text))
        requestRedraw()
    }

    fun replaceText(text: String) {
        if (children.isEmpty()) {
            text(text)
        } else {
            (children.first() as? TextNode)?.text = text
            requestRedraw()
        }
    }
}

fun TextElement.bind(property: KMutableProperty0<String>, program: Program? = null): Binding0<Unit, String> {
    val program = program ?: (root() as? Body)?.controlManager?.program ?: error("no program")
    return Binding0(program, this, Event<Unit>(), property, { "" }, { this.replaceText(it) })
}

