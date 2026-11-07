package org.openrndr.panel.style

import org.openrndr.extra.style.parseCssAngle
import org.openrndr.extra.style.splitCss
import org.openrndr.extra.style.splitCssWords
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import kotlin.math.tan

/**
 * A transform function of CSS: [name] is translate, scale, rotate or skew, [arguments] are its x and y values: lengths
 * for translate, in pixels or percentages of the size of the element, factors for scale and degrees for rotate and skew
 */
data class TransformFunction(val name: String, val arguments: List<LinearDimension>) {
    /** the function that does nothing, with the same name */
    fun identity() = TransformFunction(name, List(arguments.size) { if (name == "scale") LinearDimension.PX(1.0) else LinearDimension.PX(0.0) })
}

/** the transform of an element, the functions are applied from right to left, see [transform] */
data class Transform(val functions: List<TransformFunction>) : PropertyValue() {
    /** the matrix of the transform, for an element of [size] */
    fun matrix(size: Vector2): Matrix44 {
        fun value(d: LinearDimension, full: Double) = when (d) {
            is LinearDimension.PX -> d.value
            is LinearDimension.Percent -> d.value / 100.0 * full
            else -> 0.0
        }
        return transform {
            for (f in functions) {
                val x = f.arguments.getOrNull(0)
                val y = f.arguments.getOrNull(1)
                when (f.name) {
                    "translate" -> translate(value(x!!, size.x), value(y!!, size.y))
                    "scale" -> scale(value(x!!, 1.0), value(y!!, 1.0), 1.0)
                    "rotate" -> rotate(org.openrndr.math.Vector3.UNIT_Z, value(x!!, 0.0))
                    "skew" -> multiply(Matrix44(
                        1.0, tan(Math.toRadians(value(x!!, 0.0))), 0.0, 0.0,
                        tan(Math.toRadians(value(y!!, 0.0))), 1.0, 0.0, 0.0,
                        0.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 1.0
                    ))
                }
            }
        }
    }

    companion object {
        val NONE = Transform(emptyList())
    }
}

/** none, or a list of translate(), translateX(), translateY(), scale(), scaleX(), scaleY(), rotate(), skew(), skewX() and skewY() */
internal fun parseTransform(text: String): Transform? {
    val v = text.trim()
    if (v.lowercase() == "none") return Transform.NONE
    val functions = splitCssWords(v).map { word ->
        val name = word.substringBefore('(').lowercase()
        if (!word.endsWith(")")) return null
        val arguments = splitCss(word.substring(name.length + 1, word.length - 1), ',').map { it.trim() }
        fun length(a: String) = parseLength(a)?.takeIf { it is LinearDimension.PX || it is LinearDimension.Percent }
        fun number(a: String) = parseNumber(a)?.let { LinearDimension.PX(it) }
        fun angle(a: String) = (if (a == "0") 0.0 else a.takeIf { it.any(Char::isLetter) }?.let(::parseCssAngle))?.let { LinearDimension.PX(it) }
        val zero = LinearDimension.PX(0.0)
        when (name) {
            "translate" -> TransformFunction("translate", listOf(length(arguments[0]) ?: return null, arguments.getOrNull(1)?.let { length(it) ?: return null } ?: zero))
            "translatex" -> TransformFunction("translate", listOf(length(arguments[0]) ?: return null, zero))
            "translatey" -> TransformFunction("translate", listOf(zero, length(arguments[0]) ?: return null))
            "scale" -> number(arguments[0])?.let { x -> TransformFunction("scale", listOf(x, arguments.getOrNull(1)?.let { number(it) ?: return null } ?: x)) }
            "scalex" -> TransformFunction("scale", listOf(number(arguments[0]) ?: return null, LinearDimension.PX(1.0)))
            "scaley" -> TransformFunction("scale", listOf(LinearDimension.PX(1.0), number(arguments[0]) ?: return null))
            "rotate" -> TransformFunction("rotate", listOf(angle(arguments[0]) ?: return null))
            "skew" -> TransformFunction("skew", listOf(angle(arguments[0]) ?: return null, arguments.getOrNull(1)?.let { angle(it) ?: return null } ?: zero))
            "skewx" -> TransformFunction("skew", listOf(angle(arguments[0]) ?: return null, zero))
            "skewy" -> TransformFunction("skew", listOf(zero, angle(arguments[0]) ?: return null))
            else -> null
        } ?: return null
    }
    return Transform(functions)
}

/** the point of an element that it is transformed around, in fractions of its size: keywords and percentages */
internal fun parseTransformOrigin(text: String): Vector2? {
    val words = splitCssWords(text.trim().lowercase())
    fun fraction(word: String): Double? = when (word) {
        "left", "top" -> 0.0
        "center" -> 0.5
        "right", "bottom" -> 1.0
        else -> (parseLength(word) as? LinearDimension.Percent)?.value?.div(100.0)
    }
    return when (words.size) {
        1 -> when (words[0]) {
            "top", "bottom" -> Vector2(0.5, fraction(words[0])!!)
            else -> fraction(words[0])?.let { Vector2(it, 0.5) }
        }
        2 -> {
            val (a, b) = if (words[0] in setOf("top", "bottom") || words[1] in setOf("left", "right")) words[1] to words[0] else words[0] to words[1]
            Vector2(fraction(a) ?: return null, fraction(b) ?: return null)
        }
        else -> null
    }
}

/** [a] and [b] interpolated at [t], when they have the same functions; a transform of none is the identity of the other */
internal fun mixTransforms(a: Transform, b: Transform, t: Double): Transform? {
    val from = if (a.functions.isEmpty()) Transform(b.functions.map { it.identity() }) else a
    val to = if (b.functions.isEmpty()) Transform(a.functions.map { it.identity() }) else b
    if (from.functions.map { it.name to it.arguments.size } != to.functions.map { it.name to it.arguments.size }) return null
    return Transform(from.functions.zip(to.functions).map { (f, g) ->
        TransformFunction(f.name, f.arguments.zip(g.arguments).map { (x, y) ->
            when {
                x is LinearDimension.PX && y is LinearDimension.PX -> LinearDimension.PX(x.value + (y.value - x.value) * t)
                x is LinearDimension.Percent && y is LinearDimension.Percent -> LinearDimension.Percent(x.value + (y.value - x.value) * t)
                else -> if (t < 0.5) x else y
            }
        })
    })
}
