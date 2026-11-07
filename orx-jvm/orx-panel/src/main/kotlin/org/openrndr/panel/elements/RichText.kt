package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.panel.PanelText
import org.openrndr.panel.style.*

/**
 * A run of text in [RichText], styled by its own style sheets, like `span.code { font-family: Mono; color: #7fd1b9 }`:
 * its font, size, color, letter spacing and decoration. Clicks on it trigger its [mouse] events, for links.
 */
class Span(text: String) : Element(ElementType("span")) {
    /** the text, a change lays out the rich text again */
    var text: String = text
        set(value) {
            if (value != field) {
                field = value
                parent?.requestRedraw()
                parent?.requestLayout()
            }
        }

    override fun draw(drawer: Drawer) {}
}

/**
 * A paragraph of [Span]s in their own styles that wrap together, like `<p>` with `<span>`s; it is as tall as its lines
 * at its width.
 */
class RichText : Element(ElementType("rich-text")) {
    /** the spans of the text, in order */
    val spans: List<Span> get() = children.filterIsInstance<Span>()

    override val laysOutChildren: Boolean get() = false

    private val text: PanelText get() = panelText ?: error("no control manager")

    private fun runs() = spans.map { PanelText.Run(it.text, it.computedStyle) }

    private fun shaped(width: Double) = text.richParagraph(runs(), width, computedStyle.computedTextHorizontalAlign)

    override val widthHint: Double?
        get() = if (panelText == null) null else shaped(1E6).shaped.width + 1.0

    override fun heightForWidth(width: Double): Double? = if (panelText == null) null else shaped(width).shaped.height

    init {
        // a press on a span makes the text the target of the click, a click on a span is a click on the span
        mouse.pressed.listen { event ->
            if (spanAt(event.position) != null) event.cancelPropagation()
        }
        mouse.clicked.listen { event ->
            spanAt(event.position)?.let { span ->
                span.mouse.clicked.trigger(event)
            }
        }
    }

    /** the span under [position], in the coordinates of the window, null when there is none */
    fun spanAt(position: Vector2): Span? {
        val rich = shaped(layout.contentBounds.width)
        val local = position - Vector2(layout.contentBounds.x, layout.contentBounds.y + rich.shaped.ascent)
        val spans = spans
        for ((i, glyph) in rich.shaped.glyphs.withIndex()) {
            val x0 = glyph.position.x
            val x1 = glyph.penEnd?.x ?: x0
            if (local.x in x0..x1 && local.y in glyph.position.y - rich.shaped.ascent..glyph.position.y - rich.shaped.descent) {
                return spans.getOrNull(rich.runOfGlyph[i])
            }
        }
        return null
    }

    override fun draw(drawer: Drawer) {
        val content = layout.contentBoundsAtOrigin
        val rich = shaped(content.width)
        val spans = spans
        if (spans.isEmpty()) return
        val colors = spans.map { (it.computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE }
        val origin = Vector2(content.x, content.y + rich.shaped.ascent)
        text.draw(drawer, this, "text", rich.shaped, origin, ColorRGBa.WHITE, glyphColors = rich.runOfGlyph.map { colors[it] })
        // the decorations of the spans, under, over and through their own glyphs
        for ((s, span) in spans.withIndex()) {
            val decoration = span.computedStyle.textDecoration
            if (decoration.isNone) continue
            val glyphs = rich.runOfGlyph.indices.filter { rich.runOfGlyph[it] == s }.toSet()
            text.drawDecoration(drawer, rich.shaped, Vector2(origin.x, kotlin.math.round(origin.y)), colors[s], decoration, glyphs)
        }
    }
}

/** a [RichText] that [init] adds spans to */
fun Element.richText(vararg classes: String, init: RichText.() -> Unit): RichText = initElement(classes, RichText(), init)

/** a [Span] of the text that [text] gives */
fun RichText.span(vararg classes: String, text: () -> String): Span = initElement(classes, Span(text())) {}
