package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.style.TransitionTiming
import org.openrndr.extra.style.Transitions
import org.openrndr.panel.elements.Element
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.transitionDelay
import org.openrndr.panel.style.transitionDuration
import org.openrndr.panel.style.transitionProperty
import org.openrndr.panel.style.transitionTimingFunction

/** the properties that transition: colors and the opacity */
private val transitioned = listOf("background-color", "color", "border-color", "outline-color", "opacity")

/**
 * The transitions of the styles of elements with a [org.openrndr.panel.style.transitionDuration]: when a property
 * that transitions changes, for example by a hover state, its value moves to the new value over the duration, after the
 * [org.openrndr.panel.style.transitionDelay], with the [org.openrndr.panel.style.transitionTimingFunction], as CSS
 * transitions do; see [Transitions]. Runs after the styles are computed, and sets the values of the transitions in the
 * computed styles.
 * @param clock the time in seconds, real time by default
 */
internal class StyleTransitions(private val clock: () -> Double = { System.nanoTime() / 1E9 }) {
    private class State {
        /** the values the styles gave the properties last */
        val targets = mutableMapOf<String, Any>()
        val transitions = Transitions<Any> { _, a, b, t -> mix(a, b, t) }
    }

    private val states = java.util.WeakHashMap<Element, State>()

    /** the elements with transitions that run, they are drawn again */
    var animating: List<Element> = emptyList()
        private set

    /** the value of a property, a color or an opacity, null for other values */
    private fun valueOf(value: Any?): Any? = when (value) {
        is Color.RGBa -> value.color
        is Double -> value
        else -> null
    }

    /** applies the transitions to the styles of [root] and its descendants that were computed again */
    fun apply(root: Element) {
        val now = clock()
        val result = mutableListOf<Element>()
        fun visit(element: Element) {
            // computed styles that were not computed again keep the values of their transitions, elements with
            // transitions that run are computed again in every frame
            if (!element.styleFresh) {
                element.children.forEach(::visit)
                return
            }
            element.styleFresh = false
            val cs = element.computedStyle
            val duration = cs.transitionDuration
            val state = states[element] ?: if (duration > 0.0) State().also { states[element] = it } else null
            if (state != null) {
                val properties = cs.transitionProperty
                for (name in transitioned) {
                    // an opacity that is not set is 1
                    val target = valueOf(cs.getProperty(name)?.value ?: if (name == "opacity") 1.0 else null) ?: continue
                    val previous = state.targets.put(name, target)
                    if (previous != null && previous != target) {
                        val timing = if (duration > 0.0 && ("all" in properties || name in properties))
                            TransitionTiming(duration, cs.transitionDelay, cs.transitionTimingFunction) else null
                        state.transitions.change(name, previous, target, now, timing)
                    }
                    val value = state.transitions.value(name, now) ?: continue
                    cs.setProperty(name, if (value is ColorRGBa) Color.RGBa(value) else value)
                }
                if (state.transitions.isRunning) result.add(element)
            }
            element.children.forEach(::visit)
        }
        visit(root)
        animating = result
    }
}

private fun mix(from: Any, to: Any, t: Double): Any = when {
    from is ColorRGBa && to is ColorRGBa -> from.mix(to.toLinearity(from.linearity), t)
    from is Double && to is Double -> from + (to - from) * t
    else -> to
}
