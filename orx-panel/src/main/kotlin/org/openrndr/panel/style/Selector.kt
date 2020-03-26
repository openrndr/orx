package org.openrndr.panel.style

import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.ElementClass
import org.openrndr.panel.elements.ElementPseudoClass
import org.openrndr.panel.elements.ElementType

data class SelectorPrecedence(var inlineStyle: Int = 0, var id: Int = 0, var classOrAttribute: Int = 0, var type: Int = 0)

abstract class Selector {
    abstract fun accept(element: Element): Boolean
}

class CompoundSelector {
    companion object {
        val DUMMY = CompoundSelector()
    }
    var previous: Pair<Combinator, CompoundSelector>?
    var selectors: MutableList<Selector>

    constructor() {
        previous = null
        selectors = mutableListOf()
    }

    constructor(previous: Pair<Combinator, CompoundSelector>?, selectors: List<Selector>) {
        this.previous = previous
        this.selectors = ArrayList()
        selectors.forEach { this.selectors.add(it) }
    }

    fun precedence(p: SelectorPrecedence = SelectorPrecedence()): SelectorPrecedence {

        selectors.forEach {
            when (it) {
                is IdentitySelector -> p.id++
                is ClassSelector, is PseudoClassSelector -> p.classOrAttribute++
                is TypeSelector -> p.type++
                else -> {
                }
            }
        }
        var r = p
        previous?.let {
            r = it.second.precedence(p)
        }
        return r
    }

    override fun toString(): String {
        return "CompoundSelector(previous=$previous, selectors=$selectors)"
    }

}

enum class Combinator {
    CHILD, DESCENDANT, NEXT_SIBLING, LATER_SIBLING
}

class IdentitySelector(val id: String) : Selector() {
    override fun accept(element: Element): Boolean = if (element.id != null) {
        element.id.equals(id)
    } else {
        false
    }

    override fun toString(): String {
        return "IdentitySelector(id='$id')"
    }

}

class ClassSelector(val c: ElementClass) : Selector() {
    override fun accept(element: Element): Boolean = c in element.classes
    override fun toString(): String {
        return "ClassSelector(c=$c)"
    }
}

class TypeSelector(val type: ElementType) : Selector() {
    override fun accept(element: Element): Boolean = element.type == type
    override fun toString(): String {
        return "TypeSelector(type=$type)"
    }
}

class PseudoClassSelector(val c: ElementPseudoClass) : Selector() {
    override fun accept(element: Element): Boolean = c in element.pseudoClasses
    override fun toString(): String {
        return "PseudoClassSelector(c=$c)"
    }

}

object has {
    operator fun invoke (vararg selectors:CompoundSelector) : CompoundSelector {
        val active = CompoundSelector()
        selectors.forEach {
            active.selectors.addAll(it.selectors)
        }
        return active
    }

    infix fun state(q:String):CompoundSelector {
        val active = CompoundSelector()
        active.selectors.add(PseudoClassSelector(ElementPseudoClass((q))))
        return active
    }

    infix fun class_(q:String): CompoundSelector {
        val active = CompoundSelector()
        active.selectors.add(ClassSelector(ElementClass(q)))
        return active
    }

    infix fun type(q:String):CompoundSelector {
        val active = CompoundSelector()
        active.selectors.add(TypeSelector(ElementType(q)))
        return active
    }
}

infix fun CompoundSelector.and(other:CompoundSelector):CompoundSelector {
    val c = CompoundSelector()
    c.previous = previous
    c.selectors.addAll(selectors)
    c.selectors.addAll(other.selectors)
    return c
}