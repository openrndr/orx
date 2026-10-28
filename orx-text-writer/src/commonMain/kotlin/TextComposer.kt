package org.openrndr.extra.textwriter

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.ShapeFeature

@DslMarker
annotation class TextComposerDsl

/**
 * A DSL for composing styled text into a list of [TextSpan]s.
 *
 * The composer holds a current [style]; every [TextStyle] property is exposed as a mutable property that
 * changes the current style. Text added with [text] or [line] is captured as a span
 * using the current style. Use [styled] or [span] to change the style in a nested scope only; the
 * style is restored when the scope ends.
 *
 * Properties left `null` are not set by the composer, they will be resolved by cascading onto the style of
 * the drawer or typesetter that consumes the spans.
 *
 * ```
 * val spans = composeText {
 *     face = myFace
 *     text("CO")
 *     span("2") {
 *         sizeInEm = 0.5
 *         baselineShiftInEm = -0.25
 *     }
 *     styled {
 *         fill = ColorRGBa.PINK
 *         text(" is a gas")
 *     }
 * }
 * ```
 *
 * @param initialStyle the style the composer starts with
 */
@TextComposerDsl
class TextComposer(initialStyle: TextStyle = TextStyle()) {
    /**
     * The spans composed so far.
     */
    val spans = mutableListOf<TextSpan>()

    /**
     * The current style, used for text added from here on.
     */
    var style: TextStyle = initialStyle

    var face: Face?
        get() = style.face
        set(value) {
            style = style.copy(face = value)
        }

    var sizeInEm: Double?
        get() = style.sizeInEm
        set(value) {
            style = style.copy(sizeInEm = value)
        }

    var justify: Boolean?
        get() = style.justify
        set(value) {
            style = style.copy(justify = value)
        }

    var lineHeightInEm: Double?
        get() = style.lineHeightInEm
        set(value) {
            style = style.copy(lineHeightInEm = value)
        }

    var textWidthFactor: Double?
        get() = style.textWidthFactor
        set(value) {
            style = style.copy(textWidthFactor = value)
        }

    var characterSpacingInEm: Double?
        get() = style.characterSpacingInEm
        set(value) {
            style = style.copy(characterSpacingInEm = value)
        }

    var baselineShiftInEm: Double?
        get() = style.baselineShiftInEm
        set(value) {
            style = style.copy(baselineShiftInEm = value)
        }

    var horizontalAlign: Double?
        get() = style.horizontalAlign
        set(value) {
            style = style.copy(horizontalAlign = value)
        }

    var verticalAlign: Double?
        get() = style.verticalAlign
        set(value) {
            style = style.copy(verticalAlign = value)
        }

    var fill: ColorRGBa?
        get() = style.fill
        set(value) {
            style = style.copy(fill = value)
        }

    var stroke: ColorRGBa?
        get() = style.stroke
        set(value) {
            style = style.copy(stroke = value)
        }

    var strokeWeight: Double?
        get() = style.strokeWeight
        set(value) {
            style = style.copy(strokeWeight = value)
        }

    var strokeMode: StrokeMode?
        get() = style.strokeMode
        set(value) {
            style = style.copy(strokeMode = value)
        }

    var features: List<ShapeFeature>
        get() = style.features
        set(value) {
            style = style.copy(features = value)
        }

    var direction: Direction?
        get() = style.direction
        set(value) {
            style = style.copy(direction = value)
        }

    var language: String?
        get() = style.language
        set(value) {
            style = style.copy(language = value)
        }

    var hyphenate: Boolean?
        get() = style.hyphenate
        set(value) {
            style = style.copy(hyphenate = value)
        }

    /**
     * Add shaping [features] to the current style.
     */
    fun feature(vararg features: ShapeFeature) {
        this.features += features
    }

    /**
     * Add a shaping feature with [tag] and [value] to the current style.
     */
    fun feature(tag: String, value: Int = 1) {
        feature(ShapeFeature(tag, value))
    }

    /**
     * Add [text] using the current style.
     */
    fun text(text: String) {
        spans.add(TextSpan(text, style))
    }

    /**
     * Add [text] followed by a line break using the current style.
     */
    fun line(text: String = "") {
        text("$text\n")
    }

    /**
     * Run [block] with the current style cascaded with [style], the current style is restored afterward.
     */
    fun styled(style: TextStyle? = null, block: TextComposer.() -> Unit) {
        val saved = this.style
        this.style = saved.cascade(style)
        try {
            block()
        } finally {
            this.style = saved
        }
    }

    /**
     * Add [text] in a style that is configured by [block] (and optionally [style]), without changing
     * the current style.
     */
    fun span(text: String, style: TextStyle? = null, block: TextComposer.() -> Unit = {}) {
        styled(style) {
            block()
            this.text(text)
        }
    }
}

/**
 * Compose a list of [TextSpan]s using the [TextComposer] DSL.
 * @param style the initial style of the composer
 */
fun composeText(style: TextStyle = TextStyle(), compose: TextComposer.() -> Unit): List<TextSpan> {
    return TextComposer(style).apply(compose).spans
}
