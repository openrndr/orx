package org.openrndr.panel.style

import org.openrndr.panel.elements.Element

class Matcher {
    enum class MatchingResult {
        MATCHED, NOT_MATCHED, RESTART_FROM_CLOSEST_DESCENDANT, RESTART_FROM_CLOSEST_LATER_SIBLING
    }

    fun matches(selector: CompoundSelector, element: Element): Boolean {
        return matchesCompound(selector, element) == MatchingResult.MATCHED
    }

    private fun matchesCompound(selector: CompoundSelector, element: Element): MatchingResult {
        if (selector.selectors.any { !it.accept(element) }) {
            return MatchingResult.RESTART_FROM_CLOSEST_LATER_SIBLING
        }

        if (selector.previous == null) {
            return MatchingResult.MATCHED
        }

        val (siblings, candidateNotFound) =
                when (selector.previous?.first) {
                    Combinator.NEXT_SIBLING, Combinator.LATER_SIBLING -> Pair(true, MatchingResult.RESTART_FROM_CLOSEST_DESCENDANT)
                    else -> Pair(false, MatchingResult.NOT_MATCHED)
                }

        var node = element
        while (true) {
            val nextNode = if (siblings) node.previousSibling() else node.parent

            if (nextNode == null) {
                return candidateNotFound
            } else {
                node = nextNode
            }

            val result = matchesCompound(selector.previous?.second!!, node)

            if (result == MatchingResult.MATCHED || result == MatchingResult.NOT_MATCHED) {
                return result
            }

            when (selector.previous?.first) {
                Combinator.CHILD -> return MatchingResult.RESTART_FROM_CLOSEST_DESCENDANT
                Combinator.NEXT_SIBLING -> return result
                Combinator.LATER_SIBLING -> if (result == MatchingResult.RESTART_FROM_CLOSEST_DESCENDANT) {
                    return result
                }
            }
        }
    }
}