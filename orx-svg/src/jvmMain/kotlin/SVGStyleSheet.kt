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
        "ry" to setOf("rect", "ellipse"),
        "d" to setOf("path")
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

    /**
     * The text of the style elements of the document with [root], CDATA sections included, with the style sheets that
     * they import, relative to [base]. Statements like @charset and @namespace are left out.
     */
    private fun css(root: Element, base: java.net.URI?): String =
        root.getElementsByTag("style").joinToString("\n") { imports(it.data().ifBlank { it.wholeText() }, base, mutableSetOf()) }

    /** [css] with its @import rules replaced by the style sheets they refer to, references in them made absolute */
    private fun imports(css: String, base: java.net.URI?, seen: MutableSet<String>): String {
        val text = css.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        val result = StringBuilder()
        var position = 0
        for (match in Regex("@import\\s+(?:url\\(\\s*['\"]?([^'\")]+)['\"]?\\s*\\)|['\"]([^'\"]+)['\"])[^;]*;").findAll(text)) {
            result.append(text, position, match.range.first)
            position = match.range.last + 1
            val href = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
            val location = try {
                base?.resolve(java.net.URI(null, null, href, null)) ?: java.io.File(href).absoluteFile.toURI()
            } catch (e: Exception) {
                continue
            }
            if (!seen.add(location.toString())) continue
            val imported = try {
                location.toURL().readText()
            } catch (e: Exception) {
                logger.warn { "could not import style sheet '$href': ${e.message}" }
                continue
            }
            // references in the imported style sheet are relative to it
            val absolute = imported.replace(Regex("url\\(\\s*(['\"]?)([^'\")]+)\\1\\s*\\)")) { url ->
                val reference = url.groupValues[2].trim()
                if (reference.startsWith("#") || Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(reference)) url.value
                else "url(\"${location.resolve(java.net.URI(null, null, reference, null))}\")"
            }
            result.append(imports(absolute, location, seen))
        }
        result.append(text, position, text.length)
        return result.toString().replace(Regex("@(charset|namespace)[^;{]*;"), "")
    }

    /**
     * The @font-face rules of the style elements of the document with [root]: [resolve] maps the URL of a source to
     * the file or URL that it is loaded from. Sources in formats that cannot be loaded, SVG and EOT fonts, are left out.
     */
    fun fontFaces(root: Element, base: java.net.URI?, resolve: (String) -> String): List<org.openrndr.extra.composition.FontFace> {
        val text = css(root, base).replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
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
    /** a pointer state of an element that rules can depend on, like the :hover and :active pseudo-classes */
    enum class State { HOVER, ACTIVE }

    /**
     * A rule that applies to [element] while [condition] is in [state]: its [declarations], with the [specificity]
     * and [order] of the rule
     */
    class StateRule(
        val element: Element,
        val condition: Element,
        val state: State,
        val declarations: List<Pair<String, String>>,
        val specificity: Int,
        val order: Int
    )

    /** a keyframe of @keyframes: its [offset] from 0 to 1 and its declarations */
    class Keyframe(val offset: Double, val declarations: List<Pair<String, String>>)

    /** a rule of a style sheet: a selector, the [specificity] and [order] of the rule, and its declarations */
    class SelectorRule(val selector: String, val specificity: Int, val order: Int, val declarations: String)

    /**
     * The rules and keyframes that apply over time, see [apply], and what is needed to compute the style of elements
     * again when their classes or inline styles change: the [rules] without pointer states, the original [inline]
     * styles and the custom properties that elements inherit, [variables]
     */
    class Dynamic(
        val stateRules: List<StateRule>,
        val keyframes: Map<String, List<Keyframe>>,
        val rules: List<SelectorRule> = emptyList(),
        val inline: java.util.IdentityHashMap<Element, String> = java.util.IdentityHashMap(),
        val variables: java.util.IdentityHashMap<Element, Map<String, String>> = java.util.IdentityHashMap()
    ) {
        /**
         * The style of [element] as the rules that select it now and its inline style [inlineStyle] give it, the
         * value of its style attribute
         */
        fun style(element: Element, inlineStyle: String = inline[element] ?: ""): String {
            val matched = rules.filter { rule ->
                try { element.`is`(rule.selector) } catch (e: Exception) { false }
            }.map { Pair(Pair(it.specificity, it.order), it.declarations) }
            return merge(element, matched, inlineStyle, variables[element] ?: emptyMap()).first
        }
    }

    /** the geometry attributes of elements before geometry properties were applied to them */
    private val writtenGeometry = java.util.WeakHashMap<Element, Map<String, String?>>()

    /**
     * The style attribute of [element] for the declarations of the rules that select it, [matched], and its
     * [inline] style, with the custom properties it inherits, [inherited]: the style and the custom properties in scope
     * at the element. Geometry properties are set on the element.
     */
    private fun merge(
        element: Element,
        matched: List<Pair<Pair<Int, Int>, String>>,
        inline: String,
        inherited: Map<String, String>
    ): Pair<String, Map<String, String>> {
        val rules = matched.sortedWith(compareBy({ it.first.first }, { it.first.second })).flatMap { parse(it.second) }
        val own = parse(inline)
        val ordered = rules.filter { !it.important } + own.filter { !it.important } +
                rules.filter { it.important } + own.filter { it.important }

        // custom properties are inherited, their values can use the custom properties before them
        val variables = if (ordered.none { it.name.startsWith("--") }) inherited else
            inherited.toMutableMap().also { variables ->
                for (declaration in ordered.filter { it.name.startsWith("--") }) {
                    substitute(declaration.value, variables)?.let { variables[declaration.name] = it }
                        ?: variables.remove(declaration.name)
                }
            }
        // the geometry attributes of the element as it was written, the properties override them while they apply
        val written = writtenGeometry.getOrPut(element) {
            geometry.keys.associateWith { name -> element.attr(name).takeIf { element.hasAttr(name) } }
        }
        for ((name, value) in written) if (value == null) element.removeAttr(name) else element.attr(name, value)
        val properties = ordered.filter { !it.name.startsWith("--") }.mapNotNull { declaration ->
            val value = if ("var(" in declaration.value) substitute(declaration.value, variables) else declaration.value
            // geometry properties override the attributes of the element, like in SVG 2
            if (declaration.name in geometry) {
                if (value != null && value != "auto" && element.tagName() in geometry.getValue(declaration.name)) {
                    // path data is given as path("...") in CSS, none is no path
                    element.attr(declaration.name, if (declaration.name != "d") value else
                        ValueInterpolation.pathData(value).takeIf { it != "none" } ?: "")
                }
                null
            } else {
                "${declaration.name}:${value ?: "unset"}"
            }
        }
        return Pair(properties.joinToString(";"), variables)
    }

    private val statePseudo = Regex(":(hover|active)\\b")

    /** the @keyframes rules of [css], by name */
    private fun keyframes(css: String): Map<String, List<Keyframe>> {
        val result = mutableMapOf<String, List<Keyframe>>()
        for (match in Regex("@(?:-webkit-)?keyframes\\s+([^\\s{]+)\\s*\\{").findAll(css)) {
            var depth = 1
            var i = match.range.last + 1
            val start = i
            while (i < css.length && depth > 0) {
                if (css[i] == '{') depth++ else if (css[i] == '}') depth--
                i++
            }
            val body = css.substring(start, (i - 1).coerceAtLeast(start))
            val frames = mutableListOf<Keyframe>()
            for (block in Regex("([^{}]+)\\{([^}]*)}").findAll(body)) {
                val declarations = parse(block.groupValues[2]).map { it.name to it.value }
                for (selector in block.groupValues[1].split(",").map { it.trim() }) {
                    val offset = when {
                        selector == "from" -> 0.0
                        selector == "to" -> 1.0
                        selector.endsWith("%") -> selector.dropLast(1).toDoubleOrNull()?.div(100.0)
                        else -> null
                    } ?: continue
                    frames.add(Keyframe(offset, declarations))
                }
            }
            result[match.groupValues[1].trim('"', '\'')] = frames.sortedBy { it.offset }
        }
        return result
    }

    /**
     * The rules of [selector] with :hover or :active: the elements that the selector without them selects, each with
     * the element whose state the rule depends on, the subject or the ancestor of the compound with the pseudo-class
     */
    private fun stateRules(root: Element, selector: String, declarations: String, order: Int): List<StateRule> {
        val compounds = selector.trim().split(Regex("\\s*[>+~]\\s*|\\s+")).filter { it.isNotEmpty() }
        val index = compounds.indexOfLast { statePseudo.containsMatchIn(it) }
        if (index < 0) return emptyList()
        val state = if (statePseudo.find(compounds[index])!!.groupValues[1] == "hover") State.HOVER else State.ACTIVE
        val stripped = statePseudo.replace(selector, "")
        val conditionSelector = statePseudo.replace(compounds[index], "").ifEmpty { "*" }
        val elements = try {
            root.select(stripped)
        } catch (e: Exception) {
            logger.warn { "unsupported CSS selector '$selector'" }
            return emptyList()
        }
        val parsed = parse(declarations).map { it.name to it.value }
        return elements.mapNotNull { element ->
            val condition = if (index == compounds.lastIndex) element
                else element.parents().firstOrNull { it.`is`(conditionSelector) } ?: return@mapNotNull null
            StateRule(element, condition, state, parsed, specificity(selector), order)
        }
    }

    fun apply(root: Element, base: java.net.URI? = null): Dynamic {
        // the XML parser reads the contents of style elements as text, CDATA sections included
        val css = css(root, base)
        val matches = java.util.IdentityHashMap<Element, MutableList<Pair<Pair<Int, Int>, String>>>()
        val stateRules = mutableListOf<StateRule>()
        val selectorRules = mutableListOf<SelectorRule>()
        val inlineStyles = java.util.IdentityHashMap<Element, String>()
        val inheritedVariables = java.util.IdentityHashMap<Element, Map<String, String>>()
        if (css.isNotBlank()) {
            for (rule in rules(css)) {
                for (selector in rule.selectors) {
                    // rules for pointer states apply over time
                    if (statePseudo.containsMatchIn(selector)) {
                        stateRules.addAll(stateRules(root, selector, rule.declarations, rule.order))
                        continue
                    }
                    val elements = try {
                        root.select(selector)
                    } catch (e: Exception) {
                        logger.warn { "unsupported CSS selector '$selector'" }
                        continue
                    }
                    selectorRules.add(SelectorRule(selector, specificity(selector), rule.order, rule.declarations))
                    for (element in elements) {
                        matches.getOrPut(element) { mutableListOf() }
                            .add(Pair(Pair(specificity(selector), rule.order), rule.declarations))
                    }
                }
            }
        }

        fun visit(element: Element, inherited: Map<String, String>) {
            val inline = element.attr("style")
            inlineStyles[element] = inline
            inheritedVariables[element] = inherited
            val (style, variables) = merge(element, matches[element] ?: emptyList(), inline, inherited)
            // presentation attributes can use custom properties too
            for (attribute in element.attributes().asList()) {
                if ((attribute.key in Prop.list || attribute.key in geometry) && "var(" in attribute.value) {
                    attribute.setValue(substitute(attribute.value, variables) ?: "unset")
                }
            }
            if (element.hasAttr("style") || style.isNotEmpty()) {
                element.removeAttr("style")
                if (style.isNotEmpty()) element.attr("style", style)
            }
            element.children().forEach { visit(it, variables) }
        }
        visit(root, emptyMap())
        return Dynamic(stateRules, if (css.isNotBlank()) keyframes(css) else emptyMap(), selectorRules, inlineStyles, inheritedVariables)
    }
}
