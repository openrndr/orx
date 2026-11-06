package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element

private val logger = KotlinLogging.logger {}

/**
 * The style elements and style attributes of SVG documents: CSS rules with selectors that jsoup supports, such as
 * type, class, id, attribute and descendant selectors, !important declarations, and custom properties that are used
 * with var(). Geometry properties, like the width of a rect element, are applied to the elements. The @font-face
 * rules are read by [fontFaces], other at-rules, such as @media, are skipped.
 */
internal object SVGStyleSheet {
    private class Rule(val selectors: List<String>, val declarations: String, val order: Int)

    /** the geometry properties of SVG 2 and the elements they apply to */
    private val geometry = mapOf(
        "x" to setOf("rect", "image", "use", "svg", "foreignObject"),
        "y" to setOf("rect", "image", "use", "svg", "foreignObject"),
        "width" to setOf("rect", "image", "use", "svg", "foreignObject"),
        "height" to setOf("rect", "image", "use", "svg", "foreignObject"),
        "cx" to setOf("circle", "ellipse"),
        "cy" to setOf("circle", "ellipse"),
        "r" to setOf("circle"),
        "rx" to setOf("rect", "ellipse"),
        "ry" to setOf("rect", "ellipse")
    )

    /** a declaration of [name] with [value] */
    private class Declaration(val name: String, val value: String, val important: Boolean)

    /** the rules of [css], in the order they are given */
    private fun rules(css: String): List<Rule> {
        val text = css.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        val rules = mutableListOf<Rule>()
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
                rules.add(Rule(prelude.split(",").map { it.trim() }.filter { it.isNotEmpty() }, block, rules.size))
            }
            position = close
        }
        return rules
    }

    /** [text] split at [separator], except where the separator is in parentheses or quotes */
    private fun split(text: String, separator: Char, limit: Int = Int.MAX_VALUE): List<String> {
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

    /** the declarations of a block or a style attribute, with !important removed from their values */
    private fun parse(block: String): List<Declaration> = split(block, ';').mapNotNull { declaration ->
        val parts = split(declaration, ':', 2)
        if (parts.size < 2) return@mapNotNull null
        val name = parts[0].trim()
        var value = parts[1].trim()
        val important = Regex("!\\s*important\\s*$", RegexOption.IGNORE_CASE).find(value)
        if (important != null) value = value.substring(0, important.range.first).trim()
        if (name.isEmpty()) null else Declaration(name, value, important != null)
    }

    /** the text of the style elements of the document with [root], CDATA sections included */
    private fun css(root: Element): String =
        root.getElementsByTag("style").joinToString("\n") { it.data().ifBlank { it.wholeText() } }

    /**
     * The @font-face rules of the style elements of the document with [root]: [resolve] maps the URL of a source to
     * the file or URL that it is loaded from. Sources in formats that cannot be loaded, SVG and EOT fonts, are left out.
     */
    fun fontFaces(root: Element, resolve: (String) -> String): List<org.openrndr.extra.composition.FontFace> {
        val text = css(root).replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        return Regex("@font-face\\s*\\{([^}]*)}").findAll(text).mapNotNull { match ->
            val declarations = parse(match.groupValues[1]).associate { it.name.lowercase() to it.value }
            val family = declarations["font-family"]?.trim()?.trim('"', '\'')?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val sources = split(declarations["src"] ?: "", ',').mapNotNull { source ->
                val format = Regex("format\\(\\s*['\"]?([^'\")]+)").find(source)?.groupValues?.get(1)?.trim()
                if (format == "svg" || format == "embedded-opentype") return@mapNotNull null
                Regex("url\\(\\s*(['\"]?)(.*?)\\1\\s*\\)", RegexOption.DOT_MATCHES_ALL).find(source)?.let {
                    resolve(it.groupValues[2].trim())
                } ?: Regex("local\\((.*?)\\)").find(source)?.value
            }
            if (sources.isEmpty()) return@mapNotNull null
            // a weight, or a range of weights of a variable font
            val weights = (declarations["font-weight"] ?: "normal").trim().split(Regex("\\s+")).map {
                when (it) {
                    "normal" -> 400
                    "bold" -> 700
                    else -> it.toIntOrNull() ?: 400
                }
            }
            val italic = declarations["font-style"]?.trim()?.let { it.startsWith("italic") || it.startsWith("oblique") } ?: false
            org.openrndr.extra.composition.FontFace(family, sources, weights.first()..weights.last(), italic)
        }.toList()
    }

    /** the names and values of the declarations of a style attribute */
    fun declarations(style: String): List<Pair<String, String>> = parse(style).map { it.name to it.value }

    /** the specificity of [selector]: ids, classes, attributes and pseudo-classes, types */
    private fun specificity(selector: String): Int {
        val ids = Regex("#[\\w-]+").findAll(selector).count()
        val classes = Regex("\\.[\\w-]+|\\[[^]]*]|:[\\w-]+").findAll(selector).count()
        val types = Regex("(^|[\\s>+~])[a-zA-Z][\\w-]*").findAll(selector).count()
        return ids * 10000 + classes * 100 + types
    }

    /**
     * [value] with its var() functions replaced by the values of the custom properties in [variables], or by their
     * fallbacks. Null when a custom property is not defined and has no fallback, which makes the declaration unset.
     */
    private fun substitute(value: String, variables: Map<String, String>, depth: Int = 0): String? {
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
            val arguments = split(value.substring(start + 4, end - 1), ',', 2)
            val name = arguments[0].trim()
            val replacement = variables[name]
                ?: arguments.getOrNull(1)?.let { substitute(it.trim(), variables, depth + 1) }
                ?: return null
            result.append(value, position, start).append(replacement)
            position = end
        }
        result.append(value, position, value.length)
        return result.toString()
    }

    /**
     * Applies the rules of the style elements in the document with [root] to the elements they select, and resolves
     * the custom properties and var() functions of all elements. The declarations of an element are put in its style
     * attribute, which is moved after its other attributes, so they override presentation attributes, as in SVG. They
     * are ordered by importance and specificity: rules, the style attribute, important rules and important declarations
     * of the style attribute.
     */
    fun apply(root: Element) {
        // the XML parser reads the contents of style elements as text, CDATA sections included
        val css = css(root)
        val matches = java.util.IdentityHashMap<Element, MutableList<Pair<Pair<Int, Int>, String>>>()
        if (css.isNotBlank()) {
            for (rule in rules(css)) {
                for (selector in rule.selectors) {
                    val elements = try {
                        root.select(selector)
                    } catch (e: Exception) {
                        logger.warn { "unsupported CSS selector '$selector'" }
                        continue
                    }
                    for (element in elements) {
                        matches.getOrPut(element) { mutableListOf() }
                            .add(Pair(Pair(specificity(selector), rule.order), rule.declarations))
                    }
                }
            }
        }

        fun visit(element: Element, inherited: Map<String, String>) {
            val rules = matches[element]
                ?.sortedWith(compareBy({ it.first.first }, { it.first.second }))
                ?.flatMap { parse(it.second) } ?: emptyList()
            val inline = parse(element.attr("style"))
            val ordered = rules.filter { !it.important } + inline.filter { !it.important } +
                    rules.filter { it.important } + inline.filter { it.important }

            // custom properties are inherited, their values can use the custom properties before them
            val variables = if (ordered.none { it.name.startsWith("--") }) inherited else
                inherited.toMutableMap().also { variables ->
                    for (declaration in ordered.filter { it.name.startsWith("--") }) {
                        substitute(declaration.value, variables)?.let { variables[declaration.name] = it }
                            ?: variables.remove(declaration.name)
                    }
                }
            // presentation attributes can use custom properties too
            for (attribute in element.attributes().asList()) {
                if ((attribute.key in Prop.list || attribute.key in geometry) && "var(" in attribute.value) {
                    attribute.setValue(substitute(attribute.value, variables) ?: "unset")
                }
            }
            val properties = ordered.filter { !it.name.startsWith("--") }.mapNotNull { declaration ->
                val value = if ("var(" in declaration.value) substitute(declaration.value, variables) else declaration.value
                // geometry properties override the attributes of the element, like in SVG 2
                if (declaration.name in geometry) {
                    if (value != null && value != "auto" && element.tagName() in geometry.getValue(declaration.name)) {
                        element.attr(declaration.name, value)
                    }
                    null
                } else {
                    "${declaration.name}:${value ?: "unset"}"
                }
            }
            val style = properties.joinToString(";")
            if (element.hasAttr("style") || style.isNotEmpty()) {
                element.removeAttr("style")
                if (style.isNotEmpty()) element.attr("style", style)
            }
            element.children().forEach { visit(it, variables) }
        }
        visit(root, emptyMap())
    }
}
