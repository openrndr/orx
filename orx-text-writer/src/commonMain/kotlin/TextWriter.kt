package org.openrndr.extra.textwriter

import org.openrndr.draw.DrawStyle
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontImageMap
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmRecord

/**
 * Represents a 2D coordinate or position in a Cartesian space.
 *
 * The `Cursor` class is primarily used to track and manipulate positions,
 * typically for rendering or layout purposes. It includes `x` and `y`
 * properties for horizontal and vertical positioning, respectively.
 *
 * This class allows creating a new cursor at a specified position or
 * duplicating an existing `Cursor` instance.
 *
 * @constructor Initializes the cursor using specific `x` and `y` coordinates.
 * @param x The horizontal position. Default is 0.0.
 * @param y The vertical position. Default is 0.0.
 *
 * @constructor Initializes the cursor using an existing `Cursor` instance.
 * Copies the `x` and `y` values from the provided cursor.
 * @param cursor The `Cursor` instance to duplicate.
 */
class Cursor(var x: Double = 0.0, var y: Double = 0.0) {
    constructor(cursor: Cursor) : this(cursor.x, cursor.y)
}

/**
 * Represents a text token with specific positional and formatting attributes.
 *
 * A `TextToken` contains a segment of text along with its position, width, and tracking information.
 * It can be used to describe the layout and appearance of text in a graphical context.
 *
 * @constructor Creates a new instance of a `TextToken`.
 * @param token The text content of the token.
 * @param x The horizontal position of the token.
 * @param y The vertical position of the token.
 * @param width The width of the token.
 * @param tracking The tracking (letter spacing) applied to the token.
 */
@Suppress("unused")
@JvmRecord
data class TextToken(val token: String, val x: Double, val y: Double, val width: Double, val tracking: Double) {
    companion object {
        val END_OF_LINE = TextToken("", 0.0, 0.0, 0.0, 0.0)
    }

    /**
     * Shifts the position of the text token by the specified amounts.
     *
     * @param dx the amount to shift the token horizontally.
     * @param dy the amount to shift the token vertically.
     * @return a new [TextToken] instance with the updated position.
     */
    fun shift(dx: Double, dy: Double) = TextToken(token, x + dx, y + dy, width, tracking)
}

internal fun List<TextToken>.split(): List<List<TextToken>> {
    val result = mutableListOf<List<TextToken>>()
    var active = mutableListOf<TextToken>()

    for (token in this) {
        if (token == TextToken.END_OF_LINE) {
            if (active.isNotEmpty())
                result.add(active)
            active = mutableListOf()
        } else {
            active.add(token)
        }
    }
    if (active.isNotEmpty()) {
        result.add(active)
    }
    return result
}


/**
 * Represents the styling options for text rendering in a `TextWriter`.
 *
 * This class contains various properties to control text appearance, layout, and alignment.
 * The properties defined in this class can be used to modify the behavior of text rendering,
 * including spacing between lines and characters, text alignment, and the use of ellipses
 * for overflowing text.
 */
class WriteStyle {
    /**
     * Specifies the additional line spacing to be applied between lines of text.
     *
     * This value is added to the font's inherent leading (if available),
     * determining the vertical spacing between consecutive lines.
     * Adjust this property to customize the line height independently
     * of the font's default metrics.
     *
     * Commonly used in text rendering processes where precise control
     * over line spacing is required for layout or aesthetic purposes.
     */
    var leading = 0.0

    /**
     * Adjusts the spacing between individual characters (glyphs) in text rendering.
     *
     * This variable represents the value for additional tracking, applied to influence
     * the overall spacing or "kern" between consecutive characters in a text string.
     * A positive value increases the spacing, while a negative value decreases it.
     *
     * Tracking affects the visual appearance and layout of rendered text, playing a crucial
     * role in typography and text styling. When rendering text, this value is taken into account
     * alongside other metrics such as glyph advance width and kerning.
     */
    var tracking = 0.0

    /**
     * Represents the ellipsis string used for truncating text when it exceeds
     * the available space within a text box.
     *
     * This property can be customized to define how overflowing content is visually handled,
     * allowing for a user-defined string to signify truncation (e.g., "..." or ">>>").
     * When set to `null`, no ellipsis is applied, and text may get clipped or handled
     * differently based on the implementation.
     *
     * The `ellipsis` property is particularly useful in scenarios where text needs
     * to fit within strict boundaries while preserving visual cues about clipped content.
     */
    var ellipsis: String? = "…"

    /**
     * Controls the horizontal alignment of text within a defined bounding box.
     *
     * The value is a nullable `Double` where:
     * - `0.0` aligns the text to the left of the bounding box.
     * - `0.5` centers the text within the bounding box.
     * - `1.0` aligns the text to the right of the bounding box.
     * - Interpolated values between `0.0` and `1.0` achieve proportional alignment.
     *
     * When set to `null`, horizontal alignment is disabled and the default behavior is used.
     *
     * This property is specifically applied during the rendering of text, ensuring
     * that the horizontal positioning of text tokens is adjusted based on the value set.
     */
    var horizontalAlign: Double? = null


    /**
     * Defines the vertical alignment of text within a bounding box.
     *
     * This property is represented as a nullable `Double` value, where:
     * - `0.0` aligns the text to the top of the bounding box.
     * - `0.5` centers the text vertically within the bounding box.
     * - `1.0` aligns the text to the bottom of the bounding box.
     * - Intermediate values allow proportional alignment between the top and bottom.
     *
     * When set to `null`, vertical alignment is disabled, and the default behavior is applied.
     *
     * This property influences the final vertical positioning of text during rendering,
     * ensuring that the vertical alignment adheres to the specified value within the
     * context of the defined bounding box.
     */
    var verticalAlign: Double? = null
}

@Suppress("unused", "UNUSED_PARAMETER")
class TextWriter(val drawerRef: Drawer?) {
    var cursor = Cursor()
    var box = Rectangle(
        Vector2.ZERO, drawerRef?.width?.toDouble() ?: Double.POSITIVE_INFINITY, drawerRef?.height?.toDouble()
            ?: Double.POSITIVE_INFINITY
    )
        set(value) {
            field = value
            cursor.x = value.corner.x
            cursor.y = value.corner.y
        }

    var style = WriteStyle()
    val styleStack = ArrayDeque<WriteStyle>()

    var leading
        get() = style.leading
        set(value) {
            style.leading = value
        }

    var tracking
        get() = style.tracking
        set(value) {
            style.tracking = value
        }

    var ellipsis
        get() = style.ellipsis
        set(value) {
            style.ellipsis = value
        }

    /**
     * Represents the horizontal alignment of text within the text box managed by the `TextWriter`.
     *
     * The property is a proxy to the `horizontalAlign` field of the `WriteStyle` object associated
     * with the `TextWriter`. It controls how text is aligned horizontally within the text box:
     *
     * - `0.0` aligns text to the left.
     * - `0.5` centers text within the bounding box.
     * - `1.0` aligns text to the right.
     * - Intermediate values proportionally adjust the horizontal alignment.
     *
     * A `null` value disables explicit horizontal alignment, reverting to the default behavior.
     *
     * Modifying this property affects the layout of text rendered by the `TextWriter`
     * according to the specified alignment value.
     */
    var horizontalAlign
        get() = style.horizontalAlign
        set(value) {
            style.horizontalAlign = value
        }

    /**
     * Represents the vertical alignment of text within a bounding box.
     *
     * This property defines how text is vertically positioned in relation to the bounds
     * of a defined space. The value is directly tied to the `verticalAlign` property of
     * the associated `WriteStyle` and can influence text layout during rendering.
     *
     * The alignment can be set using a nullable `Double` value, where:
     * - `0.0` aligns text to the top of the bounding box.
     * - `0.5` centers text vertically within the bounding box.
     * - `1.0` aligns text to the bottom of the bounding box.
     * - Intermediate values allow proportional vertical alignments.
     *
     * If set to `null`, vertical alignment is disabled, and default layout behavior is applied.
     *
     * Changes to this property will immediately affect the vertical positioning of text rendered
     * using the `TextWriter` class.
     */
    var verticalAlign
        get() = style.verticalAlign
        set(value) {
            style.verticalAlign = value
        }

    /**
     * Represents the drawing style for rendering text elements.
     *
     * The `drawStyle` property encapsulates settings related to font, kerning, and text rendering options.
     * If a `drawerRef` is available, the `drawStyle` is sourced from it; otherwise, the property uses its own value.
     *
     * In the context of the `TextWriter` class, this property is utilized in text layout and rendering calculations,
     * including operations like determining line height, character spacing, and overall text dimensions.
     */
    var drawStyle: DrawStyle = DrawStyle()
        get() {
            return drawerRef?.drawStyle ?: field
        }
        set(value) {
            field = drawStyle
        }

    /**
     * Moves the cursor position to the start of the next line within the defined text box.
     * The horizontal position of the cursor is reset to align with the left edge of the text box,
     * while the vertical position is incremented by the sum of the font's leading value (if available)
     * and the additional line spacing defined in the style settings.
     *
     * This function is commonly used as part of a text rendering process to ensure proper
     * vertical alignment of subsequent lines of text.
     */
    fun newLine() {
        require(style.verticalAlign == null) { "Not allowed to use newLine() with verticalAlign set" }

        cursor.x = box.corner.x
        cursor.y += (drawStyle.fontMap?.leading ?: 0.0) + style.leading
    }

    /**
     * Moves the cursor to the beginning of the next line without considering additional line spacing
     * or style-specific adjustments like leading. The cursor's x-coordinate is reset to the left edge
     * of the defined text box, while its y-coordinate is incremented by the height of the current font,
     * if available, or remains unchanged if no font is set.
     *
     * This method is useful for situations where precise control over cursor positioning is required,
     * bypassing the additional spacing typically applied by other line management methods.
     */
    fun gaplessNewLine() {
        cursor.x = box.corner.x
        cursor.y += drawStyle.fontMap?.height ?: 0.0
    }

    /**
     * Moves the cursor by the specified horizontal and vertical offsets.
     *
     * @param x The horizontal offset to move the cursor by. A positive value moves the cursor to the right,
     *          and a negative value moves it to the left.
     * @param y The vertical offset to move the cursor by. A positive value moves the cursor downward,
     *          and a negative value moves it upward.
     */
    fun move(x: Double, y: Double) {
        cursor.x += x
        cursor.y += y
    }

    /**
     * Calculates the total width of a text string based on the glyph metrics and style settings.
     *
     * @param text the input text string whose width is to be calculated
     * @return the total width of the text as a Double value
     */
    fun textWidth(text: String): Double =
        text.sumOf {
            ((drawStyle.fontMap as FontImageMap).glyphMetrics[it]?.advanceWidth ?: 0.0) + style.tracking
        } - (text.count { it == ' ' } + 1) * style.tracking

    /**
     * Processes a list of text strings to generate text tokens and optionally renders them.
     *
     * This method joins the provided list of strings using newline characters, then calculates
     * and returns a list of `TextToken` instances representing the layout and typesetting results.
     * If the `visible` parameter is set to true, the text is also rendered visually.
     *
     * @param text a list of strings to be combined and processed as text
     * @param visible determines whether the text should be rendered (true) or just typeset (false). Default is true.
     * @return a list of `TextToken` instances representing the processed text tokens
     */
    fun text(text: List<String>, visible: Boolean = true): List<TextToken> {
        return text(text.joinToString("\n"), visible)
    }

    /**
     * Draw text
     * @param text the text to write, may contain newlines
     * @param visible draw the text when set to true, when set to false only type setting is performed
     * @return a list of [TextToken] instances
     */
    fun text(text: String, visible: Boolean = true): List<TextToken> {

        if (style.horizontalAlign != null) {
            require(cursor.x == box.corner.x) { "cursor must be at the box's left edge for horizontal alignment" }
        }

        // Triggers loading the default font (if needed) by accessing .fontMap
        // otherwise makeRenderTokens() is not aware of the default font.
        val fontMap = drawerRef?.fontMap ?: error("no fontmap")

        var renderTokens = makeTextTokens(text, false)

        renderTokens = when (val align = style.horizontalAlign) {
            null -> {
                renderTokens
            }

            else -> {
                renderTokens.split().flatMap {
                    val first = it.first()
                    val last = it.last()

                    val sx = first.x
                    val ex = last.x + last.width

                    val tw = ex - sx
                    val shift = (box.width - tw) * align

                    it.map { it.shift(shift, 0.0) } //+ listOf(TextToken.END_OF_LINE)
                }
            }
        }

        renderTokens = when (val align = style.verticalAlign) {
            null -> {
                renderTokens
            }

            else -> {
                val first = renderTokens.filter { it != TextToken.END_OF_LINE }.first()
                val last = renderTokens.last()
                renderTokens.split().flatMap {
                    val sy =  first.y - fontMap.ascenderLength
                    val ey = last.y + fontMap.descenderLength

                    val th = ey - sy
                    it.map { it.shift(0.0, fontMap.height + (box.height - th) * align) }
                }
            }
        }


        if (visible) {
            drawTextTokens(renderTokens)
        }
        return renderTokens
    }

    /**
     * Draw pre-set text tokens.
     * @param tokens a list of [TextToken] instances
     * @since 0.4.3
     */
    fun drawTextTokens(tokens: List<TextToken>) {
        drawerRef?.let { d ->
            val renderer = d.fontImageMapDrawer
            val queue = renderer.getQueue(tokens.sumOf { it.token.length })
            tokens.forEach {
                renderer.queueText(
                    fontMap = d.drawStyle.fontMap!!,
                    text = it.token,
                    x = it.x,
                    y = it.y,
                    tracking = style.tracking,
                    kerning = drawStyle.kerning,
                    textSetting = drawStyle.textSetting,
                    queue
                )
            }
            renderer.flush(d.context, d.drawStyle, queue)
        }
    }

    private fun makeTextTokens(text: String, mustFit: Boolean = false): List<TextToken> {
        drawStyle.fontMap?.let { font ->

            var fits = true
            font as FontImageMap
            val lines = text.split("((?<=\n)|(?=\n))".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val tokens = mutableListOf<String>()
            lines.forEach { line ->
                val lineTokens = line.split(" ")
                tokens.addAll(lineTokens)
            }

            val localCursor = Cursor(cursor)

            val spaceWidth = (font.glyphMetrics[' ']?.advanceWidth ?: error("no metrics for space"))
            val verticalSpace = style.leading + font.leading

            val textTokens = mutableListOf<TextToken>()

            tokenLoop@ for (i in 0 until tokens.size) {
                val token = tokens[i]
                if (token == "\n") {
                    textTokens.add(TextToken.END_OF_LINE)
                    localCursor.x = box.corner.x
                    localCursor.y += verticalSpace
                } else {
                    val tokenWidth = token.sumOf {
                        (font.glyphMetrics[it]?.advanceWidth ?: 0.0)
                    } + style.tracking * (token.length - 1).coerceAtLeast(0)
                    if (localCursor.x + tokenWidth < box.x + box.width && localCursor.y <= box.y + box.height) run {
                        val textToken = TextToken(token, localCursor.x, localCursor.y, tokenWidth, style.tracking)
                        textTokens.add(textToken)
                    } else {
                        if (localCursor.y > box.corner.y + box.height) {
                            fits = false
                        }
                        if (localCursor.y + verticalSpace <= box.y + box.height) {
                            textTokens.add(TextToken.END_OF_LINE)
                            localCursor.y += verticalSpace
                            localCursor.x = box.x

                            textTokens.add(
                                TextToken(token, localCursor.x, localCursor.y, tokenWidth, style.tracking)
                            )
                        } else {
                            if (!mustFit && style.ellipsis != null && cursor.y <= box.y + box.height) {
                                textTokens.add(
                                    TextToken(
                                        style.ellipsis
                                            ?: "", localCursor.x, localCursor.y, tokenWidth, style.tracking
                                    )
                                )
                                break@tokenLoop
                            } else {
                                fits = false
                            }
                        }
                    }
                    localCursor.x += tokenWidth

                    if (i != tokens.lastIndex) {
                        localCursor.x += spaceWidth + tracking
                    }
                }
            }
            if (fits || (!fits && !mustFit)) {
                cursor = Cursor(localCursor)
            } else {
                textTokens.clear()
            }

            return textTokens
        }
        return emptyList()
    }


}

/**
 * Executes a block of code using a [TextWriter] instance initialized with the provided [Drawer].
 * The block is guaranteed to be invoked exactly once.
 *
 * @param T The return type of the block.
 * @param drawer The [Drawer] object used to initialize the [TextWriter].
 * @param f The block of code to be executed with the [TextWriter] receiver.
 * @return The result of the executed block.
 */
@OptIn(ExperimentalContracts::class)
fun <T> writer(drawer: Drawer, f: TextWriter.() -> T): T {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    val textWriter = TextWriter(drawer)
    return textWriter.f()
}