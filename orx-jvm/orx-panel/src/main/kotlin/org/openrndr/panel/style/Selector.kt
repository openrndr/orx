package org.openrndr.panel.style

import org.openrndr.extra.style.ComplexSelector
import org.openrndr.extra.style.SelectorTree
import org.openrndr.extra.style.SimpleSelector
import org.openrndr.extra.style.Specificity
import org.openrndr.extra.style.parseCssSelector
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.ElementClass
import org.openrndr.panel.elements.ElementPseudoClass
import org.openrndr.extra.style.CompoundSelector as CssCompoundSelector

/** how a compound selector relates to the one before it: descendant, child, next sibling or later sibling */
typealias Combinator = org.openrndr.extra.style.Combinator

/**
 * A selector of elements, the last compound of a selector of CSS: the [selectors] that the element matches, and the
 * [previous] compound with the combinator that relates them, as `.sidebar > button:hover`. Build them with [has] and
 * [and], or from CSS with [CompoundSelector.parse]; they match with the selectors of CSS, see [ComplexSelector].
 */
class CompoundSelector(
    var previous: Pair<Combinator, CompoundSelector>? = null,
    selectors: List<SimpleSelector> = emptyList()
) {
    val selectors: MutableList<SimpleSelector> = selectors.toMutableList()

    companion object {
        /** the selector that selects every element */
        val DUMMY = CompoundSelector()

        /** the selector of the CSS selector [css], like `.toolbar > button:hover`; null when it is not valid */
        fun parse(css: String): CompoundSelector? = parseCssSelector(css)?.let(::of)

        /** the selector of the CSS selector [selector] */
        fun of(selector: ComplexSelector): CompoundSelector {
            var result: CompoundSelector? = null
            for ((i, compound) in selector.compounds.withIndex()) {
                result = CompoundSelector(result?.let { selector.combinators[i - 1] to it }, compound.simples)
            }
            return result!!
        }
    }

    /** the selector of CSS, from the first compound to this */
    fun toCss(): ComplexSelector {
        val chain = generateSequence(this) { it.previous?.second }.toList().reversed()
        val combinators = generateSequence(this) { it.previous?.second }.mapNotNull { it.previous?.first }.toList().reversed()
        return ComplexSelector(chain.map { CssCompoundSelector(it.selectors.toList()) }, combinators)
    }

    /** the specificity of the selector */
    val specificity: Specificity get() = toCss().specificity

    override fun toString(): String = toCss().toString()
}

/** how the selectors of CSS see the elements of panels: states are their pseudo-classes, like hover and focus */
object PanelSelectorTree : SelectorTree<Element> {
    override fun parent(node: Element): Element? = node.parent
    override fun children(node: Element): List<Element> = node.children
    override fun type(node: Element): String = node.type.name
    override fun id(node: Element): String? = node.id
    override fun hasClass(node: Element, name: String): Boolean = ElementClass(name) in node.classes
    override fun attribute(node: Element, name: String): String? = node.attribute(name)
    override fun hasState(node: Element, name: String): Boolean = ElementPseudoClass(name) in node.pseudoClasses
}

/** true when [selector] selects [element] */
fun ComplexSelector.matches(element: Element): Boolean = matches(element, PanelSelectorTree)

object has {
    operator fun invoke(vararg selectors: CompoundSelector): CompoundSelector =
        CompoundSelector(selectors = selectors.flatMap { it.selectors })

    /** elements in the state [q], like hover, a pseudo-class */
    infix fun state(q: String) = CompoundSelector(selectors = listOf(SimpleSelector.PseudoClass(q)))

    infix fun class_(q: String) = CompoundSelector(selectors = listOf(SimpleSelector.Class(q)))

    infix fun type(q: String) = CompoundSelector(selectors = listOf(SimpleSelector.Type(q)))

    infix fun id(q: String) = CompoundSelector(selectors = listOf(SimpleSelector.Id(q)))

    /** elements of any of the types [qs], like :is() of the types */
    infix fun type(qs: Iterable<String>) = CompoundSelector(
        selectors = listOf(SimpleSelector.Logical("is", qs.map { ComplexSelector(listOf(CssCompoundSelector(listOf(SimpleSelector.Type(it)))), emptyList()) }))
    )
}

infix fun CompoundSelector.and(other: CompoundSelector): CompoundSelector =
    CompoundSelector(previous, selectors + other.selectors)
