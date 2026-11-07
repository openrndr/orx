package org.openrndr.extra.style

/** the keywords that every CSS property takes */
enum class CssKeyword(val css: String) {
    /** the value of the parent */
    INHERIT("inherit"),

    /** the initial value of the property */
    INITIAL("initial"),

    /** the value of the parent for properties that are inherited, the initial value for others */
    UNSET("unset");

    companion object {
        /** the keyword [text] is, null when it is not one */
        fun of(text: String): CssKeyword? = entries.firstOrNull { it.css == text.trim().lowercase() }
    }
}

/** a declaration of CSS: the property [name] with [value] as text, [important] when it is marked !important */
data class CssDeclaration(val name: String, val value: String, val important: Boolean = false)

/** a rule of CSS: its [selectors] and the text of its [block], [order] is its place in the style sheet */
data class CssRule(val selectors: List<String>, val block: String, val order: Int) {
    /** the declarations of the block */
    val declarations: List<CssDeclaration> by lazy { parseCssDeclarations(block) }
}

/** [text] split at [separator], except where the separator is in parentheses or quotes, in at most [limit] parts */
fun splitCss(text: String, separator: Char, limit: Int = Int.MAX_VALUE): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var quote: Char? = null
    var start = 0
    for ((i, c) in text.withIndex()) {
        when {
            quote != null -> if (c == quote) quote = null
            c == '"' || c == '\'' -> quote = c
            c == '(' -> depth++
            c == ')' -> depth = (depth - 1).coerceAtLeast(0)
            c == separator && depth == 0 && parts.size < limit - 1 -> {
                parts.add(text.substring(start, i))
                start = i + 1
            }
        }
    }
    parts.add(text.substring(start))
    return parts
}

/** [text] split at whitespace that is not in parentheses or quotes, like the values of shorthands */
fun splitCssWords(text: String): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var quote: Char? = null
    val current = StringBuilder()
    for (c in text) {
        when {
            quote != null -> { current.append(c); if (c == quote) quote = null }
            c == '"' || c == '\'' -> { current.append(c); quote = c }
            c == '(' -> { depth++; current.append(c) }
            c == ')' -> { depth = (depth - 1).coerceAtLeast(0); current.append(c) }
            c.isWhitespace() && depth == 0 -> if (current.isNotEmpty()) { parts.add(current.toString()); current.clear() }
            else -> current.append(c)
        }
    }
    if (current.isNotEmpty()) parts.add(current.toString())
    return parts
}

/** the declarations of a block or a style attribute, with !important removed from their values */
fun parseCssDeclarations(block: String): List<CssDeclaration> =
    splitCss(block.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), ""), ';').mapNotNull { declaration ->
        val parts = splitCss(declaration, ':', 2)
        if (parts.size < 2) return@mapNotNull null
        val name = parts[0].trim()
        var value = parts[1].trim()
        val important = Regex("!\\s*important\\s*$", RegexOption.IGNORE_CASE).find(value)
        if (important != null) value = value.substring(0, important.range.first).trim()
        if (name.isEmpty()) null else CssDeclaration(name, value, important != null)
    }

/** the rules of the style sheet [css], in the order they are given; at-rules are left out */
fun parseCssRules(css: String): List<CssRule> {
    val text = css.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
    val rules = mutableListOf<CssRule>()
    var position = 0
    while (position < text.length) {
        val open = text.indexOf('{', position)
        if (open < 0) break
        val prelude = text.substring(position, open).trim()
        // the block ends at the matching brace, blocks of at-rules can hold blocks
        var depth = 1
        var close = open + 1
        while (close < text.length && depth > 0) {
            when (text[close]) {
                '{' -> depth++
                '}' -> depth--
            }
            close++
        }
        val block = text.substring(open + 1, (close - 1).coerceAtLeast(open + 1))
        if (prelude.isNotEmpty() && !prelude.startsWith("@")) {
            rules.add(CssRule(splitCss(prelude, ',').map { it.trim() }.filter { it.isNotEmpty() }, block, rules.size))
        }
        position = close
    }
    return rules
}

/**
 * The value of the calc() expression [value]: sums, differences, products and quotients of values that [leaf]
 * evaluates, in parentheses and nested calc(); null when it is not valid or a leaf is not known
 */
fun evaluateCssCalc(value: String, leaf: (String) -> Double?): Double? {
    val tokens = Regex("calc\\(|\\(|\\)|\\*|/|[+-](?=\\s)|[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:e[+-]?\\d+)?(?:[a-zA-Z]+|%)?")
        .findAll(value).map { it.value }.toList()
    var position = 0
    fun peek() = tokens.getOrNull(position)
    fun expression(): Double? {
        fun factor(): Double? = when (val token = tokens.getOrNull(position++)) {
            null -> null
            "calc(", "(" -> expression().takeIf { tokens.getOrNull(position++) == ")" }
            else -> leaf(token)
        }
        fun term(): Double? {
            var result = factor() ?: return null
            while (peek() == "*" || peek() == "/") {
                val operator = tokens[position++]
                val operand = factor() ?: return null
                result = if (operator == "*") result * operand else result / operand
            }
            return result
        }
        var result = term() ?: return null
        while (peek() == "+" || peek() == "-") {
            val operator = tokens[position++]
            val operand = term() ?: return null
            result = if (operator == "+") result + operand else result - operand
        }
        return result
    }
    return expression()?.takeIf { position == tokens.size && it.isFinite() }
}

/** a number with a unit of CSS, like 12px, 50%, 1.5em or 2fr; [unit] is lowercase, empty for numbers */
data class CssDimension(val value: Double, val unit: String) {
    /** the value in pixels for absolute units and numbers, as CSS defines them at 96 pixels per inch; null for others */
    val pixels: Double?
        get() = when (unit) {
            "", "px" -> value
            "in" -> value * 96.0
            "cm" -> value * 96.0 / 2.54
            "mm" -> value * 96.0 / 25.4
            "q" -> value * 96.0 / 101.6
            "pt" -> value * 4.0 / 3.0
            "pc" -> value * 16.0
            else -> null
        }
}

private val dimension = Regex("^([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)([a-zA-Z]*|%)$")

/** the number with a unit [text] is, null when it is not one */
fun parseCssDimension(text: String): CssDimension? {
    val match = dimension.matchEntire(text.trim()) ?: return null
    return CssDimension(match.groupValues[1].toDouble(), match.groupValues[2].lowercase())
}

/**
 * The value of [text] in pixels: a length in absolute units, a number, or a calc() expression of them; null when it
 * is not one
 */
fun parseCssPixels(text: String): Double? {
    val v = text.trim()
    if (v.startsWith("calc(")) return evaluateCssCalc(v) { parseCssDimension(it)?.pixels }
    return parseCssDimension(v)?.pixels
}

/**
 * A property of CSS: its [name], whether it is [inherited] by default, its [initial] value, and how a value is
 * parsed from text, null when the text is not a valid value
 */
class CssProperty<T>(val name: String, val inherited: Boolean, val initial: T, val parse: (String) -> T?)

/**
 * The properties of a style system by their names, and its shorthands: a shorthand is expanded into the declarations
 * of its longhands
 */
open class CssProperties {
    private val properties = LinkedHashMap<String, CssProperty<*>>()
    private class Shorthand(val longhands: List<String>, val expand: (String) -> List<Pair<String, String>>?)

    private val shorthands = LinkedHashMap<String, Shorthand>()

    /** registers [property], a later registration of the same name replaces it */
    fun <T> register(property: CssProperty<T>): CssProperty<T> {
        properties[property.name] = property
        return property
    }

    /**
     * Registers the shorthand [name] of [longhands], [expand] gives the values of the longhands of a value as text,
     * null when it is not valid; longhands that it leaves out are set to their initial values
     */
    fun shorthand(name: String, longhands: List<String>, expand: (String) -> List<Pair<String, String>>?) {
        shorthands[name] = Shorthand(longhands, expand)
    }

    /** the longhands of the shorthand [name], null when it is not a shorthand */
    fun longhands(name: String): List<String>? = shorthands[name.trim().lowercase()]?.longhands

    /** the property [name], null when it is not known */
    operator fun get(name: String): CssProperty<*>? = properties[name]

    /** the names of the properties */
    val names: Set<String> get() = properties.keys

    /** the properties */
    val all: Collection<CssProperty<*>> get() = properties.values

    /**
     * The values of [declaration]: of its property, or of the longhands of a shorthand. Keywords like `inherit` are
     * given as [CssKeyword]s. Null when the property is not known or the value is not valid.
     */
    fun values(declaration: CssDeclaration): List<Pair<String, Any?>>? {
        val name = declaration.name.trim().lowercase()
        val keyword = CssKeyword.of(declaration.value)
        shorthands[name]?.let { shorthand ->
            // a keyword applies to every longhand
            if (keyword != null) return shorthand.longhands.map { it to keyword }
            val given = shorthand.expand(declaration.value) ?: return null
            val values = given.map { (longhand, text) ->
                val property = properties[longhand] ?: return null
                longhand to (CssKeyword.of(text) ?: property.parse(text) ?: return null)
            }.toMap()
            return shorthand.longhands.map { it to (values[it] ?: CssKeyword.INITIAL) }
        }
        val property = properties[name] ?: return null
        if (keyword != null) return listOf(name to keyword)
        return listOf(name to (property.parse(declaration.value) ?: return null))
    }
}

/** the four sides of a box shorthand of CSS, like margin and padding: top, right, bottom and left; null when invalid */
fun cssBoxSides(value: String): List<String>? {
    val words = splitCssWords(value)
    return when (words.size) {
        1 -> listOf(words[0], words[0], words[0], words[0])
        2 -> listOf(words[0], words[1], words[0], words[1])
        3 -> listOf(words[0], words[1], words[2], words[1])
        4 -> words
        else -> null
    }
}

/**
 * [value] with its var() functions replaced by the values of the custom properties that [variables] gives by their
 * names, like `--accent`, or by their fallbacks. Null when a custom property is not defined and has no fallback, which
 * makes a declaration invalid at computed-value time, as in CSS.
 */
fun substituteCssVariables(value: String, variables: (String) -> String?): String? = substitute(value, variables, 0)

private fun substitute(value: String, variables: (String) -> String?, depth: Int): String? {
    if (depth > 16) return null
    val result = StringBuilder()
    var position = 0
    while (true) {
        val start = value.indexOf("var(", position)
        if (start < 0) break
        // the function ends at the matching parenthesis
        var level = 1
        var end = start + 4
        while (end < value.length && level > 0) {
            when (value[end]) {
                '(' -> level++
                ')' -> level--
            }
            end++
        }
        val arguments = splitCss(value.substring(start + 4, end - 1), ',', 2)
        val name = arguments[0].trim()
        val replacement = variables(name)
            ?: arguments.getOrNull(1)?.let { substitute(it.trim(), variables, depth + 1) }
            ?: return null
        result.append(value, position, start).append(replacement)
        position = end
    }
    result.append(value, position, value.length)
    return result.toString()
}

/** a keyframe of an @keyframes rule: its [offset] from 0 to 1 and its [declarations] */
data class CssKeyframe(val offset: Double, val declarations: List<CssDeclaration>)

/** the @keyframes rules of [css], by name, with their keyframes in the order of their offsets */
fun parseCssKeyframes(css: String): Map<String, List<CssKeyframe>> {
    val text = css.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
    val result = mutableMapOf<String, List<CssKeyframe>>()
    for (match in Regex("@(?:-webkit-)?keyframes\\s+([^\\s{]+)\\s*\\{").findAll(text)) {
        var depth = 1
        var i = match.range.last + 1
        val start = i
        while (i < text.length && depth > 0) {
            if (text[i] == '{') depth++ else if (text[i] == '}') depth--
            i++
        }
        val body = text.substring(start, (i - 1).coerceAtLeast(start))
        val frames = mutableListOf<CssKeyframe>()
        for (block in Regex("([^{}]+)\\{([^}]*)}").findAll(body)) {
            val declarations = parseCssDeclarations(block.groupValues[2])
            for (selector in block.groupValues[1].split(",").map { it.trim().lowercase() }) {
                val offset = when {
                    selector == "from" -> 0.0
                    selector == "to" -> 1.0
                    selector.endsWith("%") -> selector.dropLast(1).toDoubleOrNull()?.div(100.0)
                    else -> null
                } ?: continue
                frames.add(CssKeyframe(offset, declarations))
            }
        }
        result[match.groupValues[1].trim('"', '\'')] = frames.sortedBy { it.offset }
    }
    return result
}
