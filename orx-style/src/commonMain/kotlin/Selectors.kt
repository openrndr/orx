package org.openrndr.extra.style

/**
 * How selectors see the nodes of a tree, like the elements of an SVG document or of a user interface: selectors match
 * any tree through this view of it.
 */
interface SelectorTree<N> {
    /** the parent of [node], null for the root */
    fun parent(node: N): N?

    /** the children of [node], in order */
    fun children(node: N): List<N>

    /** the type of [node], like the name of an element; matched without regard to case */
    fun type(node: N): String

    /** the id of [node], null when it has none */
    fun id(node: N): String?

    /** true when [node] has the class [name] */
    fun hasClass(node: N, name: String): Boolean

    /** the value of the attribute [name] of [node], null when it has none */
    fun attribute(node: N, name: String): String? = null

    /** true when [node] is in the state of the pseudo-class [name], like hover, active, focus or disabled */
    fun hasState(node: N, name: String): Boolean = false
}

/** the specificity of a selector: its ids, its classes, attributes and pseudo-classes, and its types */
data class Specificity(val ids: Int, val classes: Int, val types: Int) : Comparable<Specificity> {
    operator fun plus(other: Specificity) = Specificity(ids + other.ids, classes + other.classes, types + other.types)

    override fun compareTo(other: Specificity): Int =
        compareValuesBy(this, other, { it.ids }, { it.classes }, { it.types })

    companion object {
        val ZERO = Specificity(0, 0, 0)
    }
}

/** how a compound selector relates to the one before it */
enum class Combinator(val css: String) {
    /** an ancestor, whitespace */
    DESCENDANT(" "),

    /** the parent, > */
    CHILD(" > "),

    /** the sibling right before, + */
    NEXT_SIBLING(" + "),

    /** a sibling before, ~ */
    LATER_SIBLING(" ~ ")
}

/** a simple selector: a type, an id, a class, an attribute or a pseudo-class */
sealed interface SimpleSelector {
    val specificity: Specificity
    fun <N> matches(node: N, tree: SelectorTree<N>): Boolean

    /** any node, * */
    data object Universal : SimpleSelector {
        override val specificity = Specificity.ZERO
        override fun <N> matches(node: N, tree: SelectorTree<N>) = true
        override fun toString() = "*"
    }

    data class Type(val name: String) : SimpleSelector {
        override val specificity = Specificity(0, 0, 1)
        override fun <N> matches(node: N, tree: SelectorTree<N>) = tree.type(node).equals(name, ignoreCase = true)
        override fun toString() = name
    }

    data class Id(val name: String) : SimpleSelector {
        override val specificity = Specificity(1, 0, 0)
        override fun <N> matches(node: N, tree: SelectorTree<N>) = tree.id(node) == name
        override fun toString() = "#$name"
    }

    data class Class(val name: String) : SimpleSelector {
        override val specificity = Specificity(0, 1, 0)
        override fun <N> matches(node: N, tree: SelectorTree<N>) = tree.hasClass(node, name)
        override fun toString() = ".$name"
    }

    /**
     * An attribute selector: [name] is present, or its value relates to [value] by [operator]: =, ~=, |=, ^=, $= or *=
     */
    data class Attribute(val name: String, val operator: String? = null, val value: String? = null, val ignoreCase: Boolean = false) : SimpleSelector {
        override val specificity = Specificity(0, 1, 0)
        override fun <N> matches(node: N, tree: SelectorTree<N>): Boolean {
            val actual = tree.attribute(node, name) ?: return false
            if (operator == null || value == null) return true
            val a = if (ignoreCase) actual.lowercase() else actual
            val v = if (ignoreCase) value.lowercase() else value
            return when (operator) {
                "=" -> a == v
                "~=" -> v.isNotEmpty() && v in a.split(Regex("\\s+"))
                "|=" -> a == v || a.startsWith("$v-")
                "^=" -> v.isNotEmpty() && a.startsWith(v)
                "$=" -> v.isNotEmpty() && a.endsWith(v)
                "*=" -> v.isNotEmpty() && v in a
                else -> false
            }
        }

        override fun toString() = if (operator == null) "[$name]" else "[$name$operator\"$value\"${if (ignoreCase) " i" else ""}]"
    }

    /**
     * A pseudo-class without arguments: the structural ones, root, empty, first-child, last-child, only-child,
     * first-of-type, last-of-type and only-of-type, and states, like hover, that the tree knows
     */
    data class PseudoClass(val name: String) : SimpleSelector {
        override val specificity = Specificity(0, 1, 0)
        override fun <N> matches(node: N, tree: SelectorTree<N>): Boolean = when (name) {
            "root" -> tree.parent(node) == null
            "empty" -> tree.children(node).isEmpty()
            "first-child" -> siblings(node, tree, false).firstOrNull() == node
            "last-child" -> siblings(node, tree, false).lastOrNull() == node
            "only-child" -> siblings(node, tree, false).size == 1
            "first-of-type" -> siblings(node, tree, true).firstOrNull() == node
            "last-of-type" -> siblings(node, tree, true).lastOrNull() == node
            "only-of-type" -> siblings(node, tree, true).size == 1
            else -> tree.hasState(node, name)
        }

        override fun toString() = ":$name"
    }

    /** :nth-child(an+b), :nth-last-child, :nth-of-type and :nth-last-of-type */
    data class Nth(val a: Int, val b: Int, val fromEnd: Boolean, val ofType: Boolean) : SimpleSelector {
        override val specificity = Specificity(0, 1, 0)
        override fun <N> matches(node: N, tree: SelectorTree<N>): Boolean {
            val siblings = siblings(node, tree, ofType)
            val index = siblings.indexOf(node).takeIf { it >= 0 } ?: return false
            val position = if (fromEnd) siblings.size - index else index + 1
            // position = a * n + b for an n >= 0
            return if (a == 0) position == b else (position - b) % a == 0 && (position - b) / a >= 0
        }

        override fun toString() = ":nth-${if (fromEnd) "last-" else ""}${if (ofType) "of-type" else "child"}(${a}n${if (b >= 0) "+" else ""}$b)"
    }

    /** :not(), :is() and :where() of a list of selectors; :where() has no specificity */
    data class Logical(val name: String, val selectors: List<ComplexSelector>) : SimpleSelector {
        override val specificity: Specificity
            get() = if (name == "where") Specificity.ZERO else selectors.maxOfOrNull { it.specificity } ?: Specificity.ZERO

        override fun <N> matches(node: N, tree: SelectorTree<N>): Boolean = when (name) {
            "not" -> selectors.none { it.matches(node, tree) }
            else -> selectors.any { it.matches(node, tree) }
        }

        override fun toString() = ":$name(${selectors.joinToString(", ")})"
    }
}

/** the siblings of [node], [node] included, of its type when [ofType] is set */
private fun <N> siblings(node: N, tree: SelectorTree<N>, ofType: Boolean): List<N> {
    val all = tree.parent(node)?.let { tree.children(it) } ?: listOf(node)
    return if (!ofType) all else tree.type(node).let { type -> all.filter { tree.type(it).equals(type, ignoreCase = true) } }
}

/** a compound selector: the simple selectors that one node matches, like `button.primary:hover` */
data class CompoundSelector(val simples: List<SimpleSelector>) {
    val specificity: Specificity get() = simples.fold(Specificity.ZERO) { s, it -> s + it.specificity }
    fun <N> matches(node: N, tree: SelectorTree<N>): Boolean = simples.all { it.matches(node, tree) }
    override fun toString() = if (simples.isEmpty()) "*" else simples.joinToString("")
}

/**
 * A complex selector of CSS: [compounds] from left to right, with the [combinators] between them, one fewer than the
 * compounds. The last compound selects the node.
 */
data class ComplexSelector(val compounds: List<CompoundSelector>, val combinators: List<Combinator>) {
    init {
        require(compounds.isNotEmpty() && combinators.size == compounds.size - 1)
    }

    val specificity: Specificity by lazy { compounds.fold(Specificity.ZERO) { s, it -> s + it.specificity } }

    /** true when the selector depends on the siblings before nodes, their styles change when their siblings change */
    val usesSiblings: Boolean by lazy {
        combinators.any { it == Combinator.NEXT_SIBLING || it == Combinator.LATER_SIBLING } ||
                simples().any { it is SimpleSelector.Nth || it is SimpleSelector.PseudoClass && it.name.endsWith("-child") || it is SimpleSelector.PseudoClass && it.name.endsWith("-of-type") } ||
                simples().any { it is SimpleSelector.Logical && it.selectors.any { s -> s.usesSiblings } }
    }

    /** the pseudo-classes of states, like hover, that the selector depends on */
    val states: Set<String> by lazy {
        simples().filterIsInstance<SimpleSelector.PseudoClass>().map { it.name }.filter { it !in structural }.toSet() +
                simples().filterIsInstance<SimpleSelector.Logical>().flatMap { l -> l.selectors.flatMap { it.states } }
    }

    private fun simples() = compounds.flatMap { it.simples }

    /** true when the selector selects [node] of [tree] */
    fun <N> matches(node: N, tree: SelectorTree<N>): Boolean = matchesAt(node, compounds.size - 1, tree)

    private fun <N> matchesAt(node: N, index: Int, tree: SelectorTree<N>): Boolean {
        if (!compounds[index].matches(node, tree)) return false
        if (index == 0) return true
        return when (combinators[index - 1]) {
            Combinator.CHILD -> tree.parent(node)?.let { matchesAt(it, index - 1, tree) } ?: false
            Combinator.DESCENDANT -> generateSequence(tree.parent(node)) { tree.parent(it) }.any { matchesAt(it, index - 1, tree) }
            Combinator.NEXT_SIBLING -> previousSiblings(node, tree).lastOrNull()?.let { matchesAt(it, index - 1, tree) } ?: false
            Combinator.LATER_SIBLING -> previousSiblings(node, tree).any { matchesAt(it, index - 1, tree) }
        }
    }

    private fun <N> previousSiblings(node: N, tree: SelectorTree<N>): List<N> {
        val siblings = tree.parent(node)?.let { tree.children(it) } ?: return emptyList()
        val index = siblings.indexOf(node)
        return if (index <= 0) emptyList() else siblings.subList(0, index)
    }

    override fun toString() = buildString {
        for ((i, compound) in compounds.withIndex()) {
            if (i > 0) append(combinators[i - 1].css)
            append(compound)
        }
    }

    private companion object {
        val structural = setOf("root", "empty", "first-child", "last-child", "only-child", "first-of-type", "last-of-type", "only-of-type")
    }
}

/** the selector list [text], like `a, b > c`; null when a selector in it is not valid or not supported */
fun parseCssSelectorList(text: String): List<ComplexSelector>? =
    splitCss(text, ',').map { parseCssSelector(it) ?: return null }

/**
 * The complex selector [text], like `g:hover > .item:not(.off)`; null when it is not valid or not supported, like
 * pseudo-elements and :has()
 */
fun parseCssSelector(text: String): ComplexSelector? = SelectorParser(text.trim()).complex()?.takeIf { it.second }?.first

private class SelectorParser(val text: String) {
    var position = 0

    fun peek(): Char? = text.getOrNull(position)
    fun skipWhitespace(): Boolean {
        val start = position
        while (peek()?.isWhitespace() == true) position++
        return position > start
    }

    fun identifier(): String? {
        val result = StringBuilder()
        while (true) {
            val c = peek() ?: break
            when {
                c == '\\' && position + 1 < text.length -> {
                    result.append(text[position + 1]); position += 2
                }
                c.isLetterOrDigit() || c == '-' || c == '_' || c.code >= 0x80 -> {
                    result.append(c); position++
                }
                else -> break
            }
        }
        return result.toString().takeIf { it.isNotEmpty() && !(it[0].isDigit()) && it != "-" }
    }

    /** the complex selector at the position, and true when the whole text was read */
    fun complex(stopAtParenthesis: Boolean = false): Pair<ComplexSelector, Boolean>? {
        val compounds = mutableListOf<CompoundSelector>()
        val combinators = mutableListOf<Combinator>()
        skipWhitespace()
        compounds.add(compound() ?: return null)
        while (true) {
            val hadWhitespace = skipWhitespace()
            val c = peek() ?: break
            if (stopAtParenthesis && (c == ')' || c == ',')) break
            val combinator = when (c) {
                '>' -> Combinator.CHILD
                '+' -> Combinator.NEXT_SIBLING
                '~' -> Combinator.LATER_SIBLING
                else -> if (hadWhitespace) Combinator.DESCENDANT else return null
            }
            if (combinator != Combinator.DESCENDANT) {
                position++
                skipWhitespace()
            }
            combinators.add(combinator)
            compounds.add(compound() ?: return null)
        }
        return ComplexSelector(compounds, combinators) to (position == text.length)
    }

    fun compound(): CompoundSelector? {
        val simples = mutableListOf<SimpleSelector>()
        when (peek()) {
            '*' -> { position++; simples.add(SimpleSelector.Universal) }
            else -> if (peek()?.let { it.isLetter() || it == '_' || it == '\\' || it.code >= 0x80 } == true) {
                simples.add(SimpleSelector.Type(identifier() ?: return null))
            }
        }
        // a namespace prefix, ns|type, is left out
        if (peek() == '|' && text.getOrNull(position + 1) != '=') {
            position++
            simples.clear()
            if (peek() == '*') { position++; simples.add(SimpleSelector.Universal) }
            else simples.add(SimpleSelector.Type(identifier() ?: return null))
        }
        while (true) {
            when (peek()) {
                '#' -> { position++; simples.add(SimpleSelector.Id(identifier() ?: return null)) }
                '.' -> { position++; simples.add(SimpleSelector.Class(identifier() ?: return null)) }
                '[' -> simples.add(attribute() ?: return null)
                ':' -> simples.add(pseudoClass() ?: return null)
                else -> break
            }
        }
        return if (simples.isEmpty()) null else CompoundSelector(simples)
    }

    fun attribute(): SimpleSelector? {
        position++ // [
        skipWhitespace()
        val name = identifier() ?: return null
        skipWhitespace()
        if (peek() == ']') { position++; return SimpleSelector.Attribute(name) }
        val operator = when {
            peek() == '=' -> "="
            text.startsWith("~=", position) || text.startsWith("|=", position) || text.startsWith("^=", position) ||
                    text.startsWith("$=", position) || text.startsWith("*=", position) -> text.substring(position, position + 2)
            else -> return null
        }
        position += operator.length
        skipWhitespace()
        val value = when (val quote = peek()) {
            '"', '\'' -> {
                val end = text.indexOf(quote, position + 1).takeIf { it >= 0 } ?: return null
                text.substring(position + 1, end).also { position = end + 1 }
            }
            else -> identifier() ?: return null
        }
        skipWhitespace()
        var ignoreCase = false
        if (peek() == 'i' || peek() == 'I' || peek() == 's' || peek() == 'S') {
            ignoreCase = peek()!!.lowercaseChar() == 'i'
            position++
            skipWhitespace()
        }
        if (peek() != ']') return null
        position++
        return SimpleSelector.Attribute(name, operator, value, ignoreCase)
    }

    fun pseudoClass(): SimpleSelector? {
        position++ // :
        // pseudo-elements are not supported
        if (peek() == ':') return null
        val name = identifier()?.lowercase() ?: return null
        if (peek() != '(') {
            return if (name in setOf("before", "after", "first-line", "first-letter")) null else SimpleSelector.PseudoClass(name)
        }
        position++
        val close = closingParenthesis() ?: return null
        val argument = text.substring(position, close).trim()
        position = close + 1
        return when (name) {
            "not", "is", "where", "matches" -> {
                val selectors = parseCssSelectorList(argument) ?: return null
                SimpleSelector.Logical(if (name == "matches") "is" else name, selectors)
            }
            "nth-child", "nth-last-child", "nth-of-type", "nth-last-of-type" -> {
                val (a, b) = nth(argument) ?: return null
                SimpleSelector.Nth(a, b, fromEnd = "last" in name, ofType = name.endsWith("of-type"))
            }
            else -> null
        }
    }

    /** the position of the parenthesis that closes the one before the position */
    fun closingParenthesis(): Int? {
        var depth = 1
        var i = position
        var quote: Char? = null
        while (i < text.length) {
            val c = text[i]
            when {
                quote != null -> if (c == quote) quote = null
                c == '"' || c == '\'' -> quote = c
                c == '(' -> depth++
                c == ')' -> { depth--; if (depth == 0) return i }
            }
            i++
        }
        return null
    }

    /** a and b of an+b, odd and even */
    fun nth(argument: String): Pair<Int, Int>? {
        val v = argument.replace(" ", "").lowercase()
        return when {
            v == "odd" -> 2 to 1
            v == "even" -> 2 to 0
            'n' !in v -> v.toIntOrNull()?.let { 0 to it }
            else -> {
                val (aText, bText) = v.split('n', limit = 2)
                val a = when (aText) {
                    "", "+" -> 1
                    "-" -> -1
                    else -> aText.toIntOrNull() ?: return null
                }
                val b = if (bText.isEmpty()) 0 else bText.toIntOrNull() ?: return null
                a to b
            }
        }
    }
}
