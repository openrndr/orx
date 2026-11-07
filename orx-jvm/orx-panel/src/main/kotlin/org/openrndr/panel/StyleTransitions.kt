package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.style.TransitionTiming
import org.openrndr.extra.style.Transitions
import org.openrndr.panel.elements.Element
import org.openrndr.panel.style.*
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.Transform
import org.openrndr.panel.style.mixTransforms
import org.openrndr.panel.style.transitionDelay
import org.openrndr.panel.style.transitionDuration
import org.openrndr.panel.style.transitionProperty
import org.openrndr.panel.style.transitionTimingFunction

/** the properties that transition: colors, the opacity and the transform */
private val transitioned = listOf("background-color", "color", "border-color", "outline-color", "opacity", "transform")

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

    /** an animation of an element: the keyframes of [name], with the values of their declarations, since [start] */
    private class Animation(val name: String, val start: Double, val frames: List<Pair<Double, Map<String, Any?>>>)

    private val animations = java.util.WeakHashMap<Element, Animation>()

    /** the elements with transitions that run, they are drawn again */
    var animating: List<Element> = emptyList()
        private set

    /** the value of a property, a color or an opacity, null for other values */
    private fun valueOf(value: Any?): Any? = when (value) {
        is Color.RGBa -> value.color
        is Double -> value
        is Transform -> value
        else -> null
    }

    /**
     * Applies the transitions and the animations to the styles of [root] and its descendants that were computed again
     * @return true when animations change properties that lay out
     */
    fun apply(root: Element): Boolean {
        val now = clock()
        val result = mutableListOf<Element>()
        var layout = false
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
                    // an opacity that is not set is 1, a transform none
                    val target = valueOf(cs.getProperty(name)?.value ?: when (name) { "opacity" -> 1.0; "transform" -> Transform.NONE; else -> null }) ?: continue
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
            // animations override transitions, as in CSS
            val (running, layoutAnimated) = animate(element, now)
            if (running && result.lastOrNull() !== element) result.add(element)
            layout = layout || layoutAnimated
            element.children.forEach(::visit)
        }
        visit(root)
        animating = result
        return layout
    }

    /**
     * Applies the animation of [element] at [now] to its computed style
     * @return whether the animation runs, and whether it changes properties that lay out
     */
    private fun animate(element: Element, now: Double): Pair<Boolean, Boolean> {
        val cs = element.computedStyle
        val name = cs.animationName
        val keyframes = if (name == "none") null else PanelKeyframes[name]
        if (keyframes == null || keyframes.isEmpty()) {
            animations.remove(element)
            return false to false
        }
        val animation = animations[element]?.takeIf { it.name == name } ?: Animation(name, now, keyframes.map { frame ->
            frame.offset to frame.declarations.flatMap { panelProperties().values(it) ?: emptyList() }.toMap()
        }).also { animations[element] = it }

        val duration = cs.animationDuration
        val iterations = cs.animationIterationCount
        val fill = cs.animationFillMode
        val local = now - animation.start - cs.animationDelay
        val active = duration * iterations
        // the progress through the keyframes, null when the animation does not apply
        val progress: Double? = when {
            local < 0.0 -> if (fill == AnimationFillMode.BACKWARDS || fill == AnimationFillMode.BOTH) directed(0.0, 0, cs.animationDirection) else null
            duration <= 0.0 || local >= active -> if (fill == AnimationFillMode.FORWARDS || fill == AnimationFillMode.BOTH) {
                // the end of the last iteration
                val last = if (iterations.isInfinite()) 0 else kotlin.math.ceil(iterations).toInt() - 1
                val fraction = if (iterations.isInfinite()) 1.0 else (iterations - last).coerceIn(0.0, 1.0)
                directed(fraction, last.coerceAtLeast(0), cs.animationDirection)
            } else null
            else -> {
                val iteration = kotlin.math.floor(local / duration).toInt()
                directed(local / duration - iteration, iteration, cs.animationDirection)
            }
        }
        val running = local < active || (duration > 0.0 && iterations.isInfinite())
        if (progress == null) return running to false

        // every property of the keyframes, between the keyframes around the progress; the values of the element are the
        // keyframes at 0% and 100% that are not given
        var layout = false
        val properties = animation.frames.flatMap { it.second.keys }.toSet()
        for (property in properties) {
            val base = cs.getProperty(property)?.value ?: panelProperties()[property]?.initial
            val frames = animation.frames.filter { property in it.second }.map { it.first to it.second[property] }.toMutableList()
            if (frames.first().first > 0.0) frames.add(0, 0.0 to base)
            if (frames.last().first < 1.0) frames.add(1.0 to base)
            val after = frames.indexOfFirst { it.first >= progress }.let { if (it < 0) frames.lastIndex else maxOf(it, 1) }
            val (a, from) = frames[after - 1]
            val (b, to) = frames[after]
            val t = if (b > a) cs.animationTimingFunction.ease(((progress - a) / (b - a)).coerceIn(0.0, 1.0)) else 1.0
            val value = mixValues(from, to, t)
            if (!sameValue(cs.getProperty(property)?.value, value)) {
                cs.setProperty(property, value)
                if (property !in paintProperties) layout = true
            }
        }
        return running to layout
    }

    /** the progress through the keyframes of [fraction] of [iteration], in [direction] */
    private fun directed(fraction: Double, iteration: Int, direction: AnimationDirection): Double = when (direction) {
        AnimationDirection.NORMAL -> fraction
        AnimationDirection.REVERSE -> 1.0 - fraction
        AnimationDirection.ALTERNATE -> if (iteration % 2 == 0) fraction else 1.0 - fraction
        AnimationDirection.ALTERNATE_REVERSE -> if (iteration % 2 == 0) 1.0 - fraction else fraction
    }
}

/** [from] and [to] of a property interpolated at [t]: colors, numbers, lengths of the same unit and transforms; others switch half way */
internal fun mixValues(from: Any?, to: Any?, t: Double): Any? = when {
    from is Color.RGBa && to is Color.RGBa -> Color.RGBa(from.color.mix(to.color.toLinearity(from.color.linearity), t))
    from is LinearDimension.PX && to is LinearDimension.PX -> LinearDimension.PX(from.value + (to.value - from.value) * t)
    from is LinearDimension.Percent && to is LinearDimension.Percent -> LinearDimension.Percent(from.value + (to.value - from.value) * t)
    from is FlexGrow.Ratio && to is FlexGrow.Ratio -> FlexGrow.Ratio(from.value + (to.value - from.value) * t)
    from is Double && to is Double -> from + (to - from) * t
    from is Transform && to is Transform -> mixTransforms(from, to, t) ?: if (t < 0.5) from else to
    else -> if (t < 0.5) from else to
}

private fun mix(from: Any, to: Any, t: Double): Any = when {
    from is ColorRGBa && to is ColorRGBa -> from.mix(to.toLinearity(from.linearity), t)
    from is Double && to is Double -> from + (to - from) * t
    from is Transform && to is Transform -> mixTransforms(from, to, t) ?: if (t < 0.5) from else to
    else -> to
}
