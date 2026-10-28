package org.openrndr.panel.elements

import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontMap
import org.openrndr.events.Event
import org.openrndr.extra.textwriter.TextToken
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.reflect.KMutableProperty0

class TextNode(var text: String) : Element(ElementType("text")) {

    override fun draw(drawer: Drawer) {
        computedStyle.let { style ->
            style.color.let {
                val fill = (it as? Color.RGBa)?.color ?: ColorRGBa.WHITE
                drawer.fill = (fill)
            }
            val fontMap = (root() as Body).controlManager?.fontManager?.font(computedStyle)
            drawer.fontMap = (fontMap)

            val writer = TextWriter(drawer)

            writer.ellipsis = null
            // writer.box = Rectangle(Vector2(layout.screenX * 0.0, layout.screenY * 0.0), layout.screenWidth, layout.screenHeight)
            writer.box = layout.contentBoundsAtOrigin

            writer.horizontalAlign = computedStyle.computedTextHorizontalAlign
            writer.verticalAlign = computedStyle.computedTextVerticalAlign
            writer.text(text)
        }
    }

    /** the font the text is drawn with */
    private fun fontMap(): FontMap =
        (root() as? Body)?.controlManager?.fontManager?.font(computedStyle) ?: error("no font manager")

    /**
     * The width of the widest line of the text when it is not wrapped.
     */
    fun textWidth(): Double {
        val writer = TextWriter(null)
        writer.drawStyle.fontMap = fontMap()
        return text.lines().maxOf { writer.textWidth(it) }
    }

    /**
     * The size of the text when it is wrapped at [width]. Like [draw], the text is laid out by [TextWriter], which
     * places it from the ascender of the first line to the descender of the last line.
     */
    fun sizeHint(width: Double = layout.screenWidth): Rectangle {
        val fontMap = fontMap()
        val writer = TextWriter(null)
        writer.box = Rectangle(0.0, 0.0, width, 1E9)
        writer.drawStyle.fontMap = fontMap
        val tokens = writer.text(text, visible = false).filter { it != TextToken.END_OF_LINE }
        // the distance between the baselines of the first and the last line
        val lines = if (tokens.isEmpty()) 0.0 else tokens.last().y - tokens.first().y
        // descenderLength is negative (below the baseline)
        val height = lines + fontMap.ascenderLength - fontMap.descenderLength
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

