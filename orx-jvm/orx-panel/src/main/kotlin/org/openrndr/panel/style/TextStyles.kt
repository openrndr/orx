package org.openrndr.panel.style

import org.openrndr.extra.style.splitCssWords

/** how white space and line breaks of text are handled: [NORMAL] wraps lines, [NOWRAP] and [PRE] do not */
enum class WhiteSpace { NORMAL, NOWRAP, PRE }

/** how text that does not fit in a single line is shown: cut, or cut with an ellipsis, … */
enum class TextOverflow { CLIP, ELLIPSIS }

/** the lines drawn with text: under it, over it and through it */
data class TextDecoration(val underline: Boolean = false, val overline: Boolean = false, val lineThrough: Boolean = false) : PropertyValue() {
    val isNone: Boolean get() = !underline && !overline && !lineThrough

    companion object {
        val NONE = TextDecoration()
    }
}

/** the space added after every character, `normal` is none; inherited */
var StyleSheet.letterSpacing by PropertyHandler<LinearDimension>("letter-spacing", PropertyInheritance.INHERIT, LinearDimension.PX(0.0),
    { if (it.trim().lowercase() == "normal") LinearDimension.PX(0.0) else parseLength(it)?.takeIf { l -> l is LinearDimension.PX } })

/**
 * The distance between the baselines of the lines of text: `normal`, [LinearDimension.Auto], the line height of the
 * font, a number or a percentage of the font size, or a length; inherited
 */
var StyleSheet.lineHeight by PropertyHandler<LinearDimension>("line-height", PropertyInheritance.INHERIT, LinearDimension.Auto, { text ->
    val v = text.trim().lowercase()
    when {
        v == "normal" -> LinearDimension.Auto
        parseNumber(v) != null -> LinearDimension.Percent(parseNumber(v)!! * 100.0)
        else -> parseLength(v)?.takeIf { it is LinearDimension.PX || it is LinearDimension.Percent }
    }
})

/** how white space and line breaks are handled, see [WhiteSpace]; inherited */
var StyleSheet.whiteSpace by PropertyHandler("white-space", PropertyInheritance.INHERIT, WhiteSpace.NORMAL,
    keywords("normal" to WhiteSpace.NORMAL, "nowrap" to WhiteSpace.NOWRAP, "pre" to WhiteSpace.PRE))

/** how text that does not fit in a single line is shown, see [TextOverflow]; inherited by the text of the element */
var StyleSheet.textOverflow by PropertyHandler("text-overflow", PropertyInheritance.INHERIT, TextOverflow.CLIP,
    keywords("clip" to TextOverflow.CLIP, "ellipsis" to TextOverflow.ELLIPSIS))

/** the lines drawn with text, like `underline` or `line-through`; inherited by the text of the element */
var StyleSheet.textDecoration by PropertyHandler<TextDecoration>("text-decoration", PropertyInheritance.INHERIT, TextDecoration.NONE, { text ->
    val words = splitCssWords(text.trim().lowercase())
    if (words == listOf("none")) TextDecoration.NONE
    else if (words.isEmpty() || words.any { it !in setOf("underline", "overline", "line-through") }) null
    else TextDecoration("underline" in words, "overline" in words, "line-through" in words)
})

/** initializes the delegates of this file, which register the text properties */
internal fun ensureTextProperties() = WhiteSpace.NORMAL
