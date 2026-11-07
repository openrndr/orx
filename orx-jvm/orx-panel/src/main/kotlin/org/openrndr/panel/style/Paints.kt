package org.openrndr.panel.style

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.style.parseCssAngle
import org.openrndr.extra.style.parseCssColor
import org.openrndr.extra.style.splitCss
import org.openrndr.extra.style.splitCssWords
import org.openrndr.math.Vector2

/** a color stop of a gradient: its [color] and its [position] along the gradient, null for an even spacing */
data class GradientStop(val color: ColorRGBa, val position: LinearDimension? = null)

/** the image of the background of an element, drawn under it: none, or a gradient */
sealed class BackgroundImage : PropertyValue() {
    object None : BackgroundImage() {
        override fun toString() = "none"
    }

    /**
     * A linear gradient: along [angle] in degrees, 0 up and 90 to the right, or towards the corner [corner], (±1, ±1)
     * with y down, when it is given
     */
    data class Linear(val angle: Double, val corner: Vector2?, val stops: List<GradientStop>, val repeating: Boolean = false) : BackgroundImage()

    /**
     * A radial gradient around [center], in fractions of the box, to its farthest corner: a circle, or an ellipse with
     * the proportions of the box
     */
    data class Radial(val circle: Boolean, val center: Vector2, val stops: List<GradientStop>, val repeating: Boolean = false) : BackgroundImage()
}

/**
 * A shadow of a box: offset by [offset], blurred by [blur], grown by [spread], in [color], or currentcolor when null;
 * outside the box, or inside it when [inset] is set
 */
data class Shadow(val offset: Vector2, val blur: Double, val spread: Double, val color: ColorRGBa?, val inset: Boolean = false)

/** the shadows of the box of an element, the first on top */
data class BoxShadow(val shadows: List<Shadow>) : PropertyValue() {
    /** how far the shadows reach beyond the box */
    val extent: Double
        get() = shadows.maxOfOrNull { kotlin.math.max(kotlin.math.abs(it.offset.x), kotlin.math.abs(it.offset.y)) + it.spread + 1.5 * it.blur + 1.0 } ?: 0.0

    companion object {
        val NONE = BoxShadow(emptyList())
    }
}

/** the color stops of a gradient: a color, and one or two positions */
private fun parseStops(parts: List<String>): List<GradientStop>? {
    val stops = mutableListOf<GradientStop>()
    for (part in parts) {
        val words = splitCssWords(part.trim())
        if (words.isEmpty()) return null
        val color = parseCssColor(words[0]) ?: return null
        val positions = words.drop(1).map { parseLength(it)?.takeIf { p -> p is LinearDimension.PX || p is LinearDimension.Percent } ?: return null }
        when (positions.size) {
            0 -> stops.add(GradientStop(color))
            1 -> stops.add(GradientStop(color, positions[0]))
            2 -> { stops.add(GradientStop(color, positions[0])); stops.add(GradientStop(color, positions[1])) }
            else -> return null
        }
    }
    return stops.takeIf { it.size >= 2 }
}

private val sides = mapOf("top" to Vector2(0.0, -1.0), "bottom" to Vector2(0.0, 1.0), "left" to Vector2(-1.0, 0.0), "right" to Vector2(1.0, 0.0))

/** a position of CSS in fractions of a box: keywords and percentages */
private fun parsePosition(words: List<String>): Vector2? {
    var x = 0.5
    var y = 0.5
    fun fraction(word: String): Double? = when (word) {
        "left", "top" -> 0.0
        "center" -> 0.5
        "right", "bottom" -> 1.0
        else -> (parseLength(word) as? LinearDimension.Percent)?.value?.div(100.0)
    }
    when (words.size) {
        1 -> when (words[0]) {
            "top", "bottom" -> y = fraction(words[0])!!
            else -> x = fraction(words[0]) ?: return null
        }
        2 -> {
            // keywords can be given in either order
            val (a, b) = if (words[0] in setOf("top", "bottom") || words[1] in setOf("left", "right")) words[1] to words[0] else words[0] to words[1]
            x = fraction(a) ?: return null
            y = fraction(b) ?: return null
        }
        else -> return null
    }
    return Vector2(x, y)
}

/** none, linear-gradient(), radial-gradient() and their repeating variants */
internal fun parseBackgroundImage(text: String): BackgroundImage? {
    val v = text.trim().lowercase()
    if (v == "none") return BackgroundImage.None
    val name = v.substringBefore('(')
    if (!v.endsWith(")")) return null
    val arguments = splitCss(v.substring(name.length + 1, v.length - 1), ',').map { it.trim() }
    val repeating = name.startsWith("repeating-")
    return when (name.removePrefix("repeating-")) {
        "linear-gradient" -> {
            val first = arguments.firstOrNull() ?: return null
            val words = splitCssWords(first)
            var angle = 180.0
            var corner: Vector2? = null
            val stops = when {
                words.firstOrNull() == "to" -> {
                    val directions = words.drop(1).map { sides[it] ?: return null }
                    val direction = directions.fold(Vector2.ZERO) { a, b -> a + b }
                    if (directions.size == 2) corner = direction else angle = when (direction) {
                        Vector2(0.0, -1.0) -> 0.0; Vector2(1.0, 0.0) -> 90.0; Vector2(0.0, 1.0) -> 180.0; else -> 270.0
                    }
                    if (directions.size !in 1..2) return null
                    arguments.drop(1)
                }
                words.size == 1 && parseCssColor(first) == null && parseCssAngle(first) != null -> {
                    angle = parseCssAngle(first)!!
                    arguments.drop(1)
                }
                else -> arguments
            }
            BackgroundImage.Linear(angle, corner, parseStops(stops) ?: return null, repeating)
        }
        "radial-gradient" -> {
            val first = arguments.firstOrNull() ?: return null
            val words = splitCssWords(first)
            val shaped = words.isNotEmpty() && (words[0] in setOf("circle", "ellipse") || words[0] == "at")
            var circle = false
            var center = Vector2(0.5, 0.5)
            if (shaped) {
                var rest = words
                if (rest[0] == "circle" || rest[0] == "ellipse") {
                    circle = rest[0] == "circle"
                    rest = rest.drop(1)
                }
                // the size is the farthest corner
                if (rest.firstOrNull() == "farthest-corner") rest = rest.drop(1)
                if (rest.isNotEmpty()) {
                    if (rest[0] != "at") return null
                    center = parsePosition(rest.drop(1)) ?: return null
                }
            }
            BackgroundImage.Radial(circle, center, parseStops(if (shaped) arguments.drop(1) else arguments) ?: return null, repeating)
        }
        else -> null
    }
}

/** none, or shadows: inset or not, an x and a y offset, an optional blur and spread, and an optional color */
internal fun parseBoxShadow(text: String): BoxShadow? {
    val v = text.trim().lowercase()
    if (v == "none") return BoxShadow.NONE
    val shadows = splitCss(v, ',').map { part ->
        val all = splitCssWords(part.trim())
        val inset = "inset" in all
        val words = all.filter { it != "inset" }
        if (all.count { it == "inset" } > 1) return null
        val color = words.firstNotNullOfOrNull { parseCssColor(it) }
        val lengths = words.filter { parseCssColor(it) == null }.map { (parseLength(it) as? LinearDimension.PX)?.value ?: return null }
        if (lengths.size !in 2..4 || words.size - lengths.size > 1) return null
        Shadow(Vector2(lengths[0], lengths[1]), lengths.getOrElse(2) { 0.0 }.coerceAtLeast(0.0), lengths.getOrElse(3) { 0.0 }, color, inset)
    }
    return BoxShadow(shadows)
}
