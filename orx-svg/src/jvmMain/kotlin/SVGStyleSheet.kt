package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element

private val logger = KotlinLogging.logger {}

/**
 * The style elements of SVG documents: CSS rules with selectors that jsoup supports, such as type, class, id,
 * attribute and descendant selectors. At-rules, such as @media and @font-face, are skipped.
 */
internal object SVGStyleSheet {
    private class Rule(val selectors: List<String>, val declarations: String, val order: Int)

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
                val declarations = block.replace(Regex("!\\s*important", RegexOption.IGNORE_CASE), "")
                rules.add(Rule(prelude.split(",").map { it.trim() }.filter { it.isNotEmpty() }, declarations, rules.size))
            }
            position = close
        }
        return rules
    }

    /** the specificity of [selector]: ids, classes, attributes and pseudo-classes, types */
    private fun specificity(selector: String): Int {
        val ids = Regex("#[\\w-]+").findAll(selector).count()
        val classes = Regex("\\.[\\w-]+|\\[[^]]*]|:[\\w-]+").findAll(selector).count()
        val types = Regex("(^|[\\s>+~])[a-zA-Z][\\w-]*").findAll(selector).count()
        return ids * 10000 + classes * 100 + types
    }

    /**
     * Applies the rules of the style elements in the document with [root] to the elements they select: their
     * declarations are put before the style attribute of the elements, which is moved after their other attributes,
     * so they override presentation attributes and are overridden by the style attribute, as in SVG.
     */
    fun apply(root: Element) {
        // the XML parser reads the contents of style elements as text, CDATA sections included
        val css = root.getElementsByTag("style").joinToString("\n") { it.data().ifBlank { it.wholeText() } }
        if (css.isBlank()) return
        val matches = LinkedHashMap<Element, MutableList<Pair<Pair<Int, Int>, String>>>()
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
        for ((element, declarations) in matches) {
            val sorted = declarations.sortedWith(compareBy({ it.first.first }, { it.first.second }))
            val css = sorted.joinToString(";") { it.second.trim().trimEnd(';') }
            val inline = element.attr("style")
            element.removeAttr("style")
            element.attr("style", if (inline.isBlank()) css else "$css;$inline")
        }
    }
}
