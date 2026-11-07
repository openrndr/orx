package org.openrndr.panel.style

import org.openrndr.extra.style.TimingFunction
import org.openrndr.extra.style.cssBoxSides
import org.openrndr.extra.style.parseCssColor
import org.openrndr.extra.style.parseCssDimension
import org.openrndr.extra.style.parseCssPixels
import org.openrndr.extra.style.parseCssTime
import org.openrndr.extra.style.splitCss
import org.openrndr.extra.style.splitCssList
import org.openrndr.extra.style.splitCssWords

/*
 * The parsers of the values of the properties of panels, from CSS text: each gives null for text that is not a value
 * of its property.
 */

/** a length: auto, a length in absolute units, a percentage, a fraction (fr), minmax() or calc() of absolute lengths */
internal fun parseLength(text: String): LinearDimension? {
    val v = text.trim().lowercase()
    if (v == "auto") return LinearDimension.Auto
    if (v.startsWith("minmax(") && v.endsWith(")")) {
        val arguments = splitCss(v.substring(7, v.length - 1), ',')
        if (arguments.size != 2) return null
        return LinearDimension.MinMax(parseLength(arguments[0]) ?: return null, parseLength(arguments[1]) ?: return null)
    }
    if (v.startsWith("calc(")) return parseCssPixels(v)?.let { LinearDimension.PX(it) }
    val dimension = parseCssDimension(v) ?: return null
    return when (dimension.unit) {
        "%" -> LinearDimension.Percent(dimension.value)
        "fr" -> LinearDimension.Fraction(dimension.value)
        // a number is a length only when it is 0
        "" -> if (dimension.value == 0.0) LinearDimension.PX(0.0) else null
        else -> dimension.pixels?.let { LinearDimension.PX(it) }
    }
}

internal fun parseColor(text: String): Color? = parseCssColor(text)?.let { Color.RGBa(it) }

/** a parser of the keywords of [values] */
internal fun <T> keywords(vararg values: Pair<String, T>): (String) -> T? {
    val map = values.toMap()
    return { map[it.trim().lowercase()] }
}

/** a number, or a percentage when [percentage] is set, where 100% is 1 */
internal fun parseNumber(text: String, percentage: Boolean = false): Double? {
    val dimension = parseCssDimension(text) ?: return null
    return when (dimension.unit) {
        "" -> dimension.value
        "%" -> if (percentage) dimension.value / 100.0 else null
        else -> null
    }
}

internal fun parseTime(text: String): Double? = text.trim().takeIf { it.endsWith("s") }?.let(::parseCssTime)

internal fun parseTimingFunction(text: String): TimingFunction? =
    text.takeIf { TimingFunction.isCss(it) }?.let(TimingFunction::css)

/** a list of names, separated by commas */
internal fun parseNames(text: String): List<String>? =
    splitCssList(text).map { it.trim() }.takeIf { names -> names.all { it.isNotEmpty() && ' ' !in it } }

internal fun parseFontWeight(text: String): Int? = when (val v = text.trim().lowercase()) {
    "normal" -> 400
    "bold" -> 700
    else -> v.toIntOrNull()?.takeIf { it in 1..1000 }
}

internal fun parseZIndex(text: String): ZIndex? = when (val v = text.trim().lowercase()) {
    "auto" -> ZIndex.Auto
    else -> v.toIntOrNull()?.let { ZIndex.Value(it) }
}

internal fun parseFlex(text: String): FlexGrow? = parseNumber(text)?.takeIf { it >= 0.0 }?.let { FlexGrow.Ratio(it) }

/** none, or a list of track sizes, with repeat(count, sizes) */
internal fun parseGridTemplate(text: String): GridTemplate? {
    val v = text.trim().lowercase()
    if (v == "none") return GridTemplate.None
    val tracks = mutableListOf<LinearDimension>()
    for (word in splitCssWords(v)) {
        if (word.startsWith("repeat(") && word.endsWith(")")) {
            val arguments = splitCss(word.substring(7, word.length - 1), ',', 2)
            val count = arguments.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
            val sizes = splitCssWords(arguments.getOrNull(1) ?: return null).map { parseLength(it) ?: return null }
            repeat(count) { tracks.addAll(sizes) }
        } else {
            tracks.add(parseLength(word) ?: return null)
        }
    }
    return if (tracks.isEmpty()) null else GridTemplate.LengthList(tracks)
}

/**
 * auto, a grid line, `span` a number of tracks, or a start line `/` an end line; grid lines are counted from 1, as in
 * CSS, the tracks of panels from 0
 */
internal fun parseGridPopulation(text: String): GridPopulation? {
    val v = text.trim().lowercase()
    if (v == "auto") return GridPopulation.Auto
    val words = splitCssWords(v)
    if (words.size == 2 && words[0] == "span") return words[1].toIntOrNull()?.takeIf { it > 0 }?.let { GridPopulation.Length(it) }
    val lines = splitCss(v, '/')
    return when (lines.size) {
        1 -> lines[0].trim().toIntOrNull()?.takeIf { it > 0 }?.let { GridPopulation.Position(it - 1) }
        2 -> {
            val start = lines[0].trim().toIntOrNull() ?: return null
            val end = lines[1].trim().toIntOrNull() ?: return null
            if (start < 1 || end <= start) null else GridPopulation.Span((start - 1) until (end - 1))
        }
        else -> null
    }
}

/** left, center, right or start, end, top, middle, bottom, or a number from 0 to 1 */
internal fun parseTextAlign(text: String): TextAlign? = when (val v = text.trim().lowercase()) {
    "left", "start", "top" -> TextAlign.Value(0.0)
    "center", "middle" -> TextAlign.Value(0.5)
    "right", "end", "bottom" -> TextAlign.Value(1.0)
    else -> parseNumber(v, percentage = true)?.let { TextAlign.Value(it) }
}

internal val parseCursor = keywords(
    "default" to Cursor.Default, "auto" to Cursor.Default, "pointer" to Cursor.Pointer, "text" to Cursor.Text,
    "crosshair" to Cursor.Crosshair
)

internal val parseOverflow = keywords(
    "visible" to Overflow.Visible, "hidden" to Overflow.Hidden, "clip" to Overflow.Hidden, "scroll" to Overflow.Scroll,
    "auto" to Overflow.Scroll
)

/** the values of the longhands of the box shorthand [name], like margin, with the sides of [value] */
internal fun boxShorthand(name: String, suffix: String = ""): (String) -> List<Pair<String, String>>? = { value ->
    cssBoxSides(value)?.let { sides ->
        listOf("top", "right", "bottom", "left").zip(sides).map { (side, text) -> "$name-$side$suffix" to text }
    }
}

/** the width and the color of a border or an outline, a line style like solid is accepted and left out */
internal fun lineShorthand(name: String): (String) -> List<Pair<String, String>>? = shorthand@{ value ->
    val result = mutableListOf<Pair<String, String>>()
    for (word in splitCssWords(value)) {
        when {
            word.lowercase() in setOf("none", "hidden", "solid", "dashed", "dotted", "double", "groove", "ridge", "inset", "outset") ->
                if (word.lowercase() == "none") result.add("$name-width" to "0")
            parseLength(word) != null || word.lowercase() in setOf("thin", "medium", "thick") ->
                result.add("$name-width" to when (word.lowercase()) { "thin" -> "1px"; "medium" -> "3px"; "thick" -> "5px"; else -> word })
            parseCssColor(word) != null -> result.add("$name-color" to word)
            else -> return@shorthand null
        }
    }
    result
}
