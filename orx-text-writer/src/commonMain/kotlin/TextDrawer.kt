package org.openrndr.extra.textwriter

import org.openrndr.draw.Drawer
import org.openrndr.draw.font.Face
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.jvm.JvmName

/**
 * Common interface for drawers that typeset text using [typesetText], see [SlugTextDrawer] and [VectorTextDrawer].
 */
interface TextDrawer {
    /**
     * The style that is cascaded into the style of every span.
     */
    var style: TextStyle



    fun addText(text: String, box: Rectangle, style: TextStyle = TextStyle()) {
        addText(listOf(TextSpan(text, style)), listOf(box))
    }

    fun addText(text: String, boxes: List<Rectangle>, style: TextStyle = TextStyle()) {
        addText(listOf(TextSpan(text, style)), boxes)
    }

    /**
     * Typesets [spans] into [boxes] and adds the result.
     */
    fun addText(spans: List<TextSpan>, boxes: List<Rectangle>) {
        addGlyphs(typesetText(spans, boxes, style).flatMap { it.glyphs })
    }

    /**
     * Adds glyphs that are already typeset, for example by [typesetText].
     */
    fun addGlyphs(glyphs: List<TypesetGlyph>)

    /**
     * Adds a single line of [text] in [face] starting at [position], without typesetting.
     */
    fun addText(face: Face, text: String, position: Vector2)

    /**
     * Removes all added text.
     */
    fun clear()

    /**
     * Draws all added text.
     */
    fun draw(drawer: Drawer)
}

/**
 * Typesets [spans] into [shapes] and adds the result. Shapes are filled from top to bottom, text flows from one
 * shape to the next, see [typesetText] for details.
 *
 * This is an extension function because on the JVM it would otherwise clash with
 * [TextDrawer.addText] for boxes.
 */
@JvmName("addTextInShapes")
fun TextDrawer.addText(spans: List<TextSpan>, shapes: List<Shape>) {
    addGlyphs(typesetText(spans, shapes, style).flatMap { it.glyphs })
}
