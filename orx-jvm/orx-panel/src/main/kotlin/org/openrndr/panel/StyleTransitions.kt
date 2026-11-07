package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.panel.elements.Element
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.transitionDuration
import kotlin.math.min

/** the properties that transition: colors and the opacity */
private val transitioned = listOf("background-color", "color", "border-color", "outline-color", "opacity")

/**
 * The transitions of the styles of elements with a [org.openrndr.panel.style.transitionDuration]: when a property
 * that transitions changes, for example by a hover state, its value moves to the new value over the duration. Runs
 * after the styles are computed, and sets the values of the transitions in the computed styles.
 * @param clock the time in seconds, real time by default
 */
internal class StyleTransitions(private val clock: () -> Double = { System.nanoTime() / 1E9 }) {
    private class Transition(val from: Any, val to: Any, val start: Double, val duration: Double)

    private class State {
        /** the values the styles gave the properties last */
        val targets = mutableMapOf<String, Any>()
        val running = mutableMapOf<String, Transition>()
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

    private fun mix(from: Any, to: Any, t: Double): Any = when {
        from is ColorRGBa && to is ColorRGBa -> from.mix(to.toLinearity(from.linearity), t)
        from is Double && to is Double -> from + (to - from) * t
        else -> to
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
                for (name in transitioned) {
                    // an opacity that is not set is 1
                    val target = valueOf(cs.getProperty(name)?.value ?: if (name == "opacity") 1.0 else null) ?: continue
                    val previous = state.targets.put(name, target)
                    val running = state.running[name]
                    // the value that is shown, of a transition that runs or the previous value
                    val shown = running?.let { mix(it.from, it.to, min(1.0, (now - it.start) / it.duration)) } ?: previous
                    if (previous != null && previous != target && duration > 0.0 && shown != null) {
                        state.running[name] = Transition(shown, target, now, duration)
                    }
                    val transition = state.running[name] ?: continue
                    val t = (now - transition.start) / transition.duration
                    if (t >= 1.0) {
                        state.running.remove(name)
                    } else {
                        val value = mix(transition.from, transition.to, t)
                        cs.setProperty(name, if (value is ColorRGBa) Color.RGBa(value) else value)
                    }
                }
                if (state.running.isNotEmpty()) result.add(element)
            }
            element.children.forEach(::visit)
        }
        visit(root)
        animating = result
    }
}
