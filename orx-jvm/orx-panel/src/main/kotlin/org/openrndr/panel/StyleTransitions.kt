package org.openrndr.panel

import org.openrndr.extra.style.TransitionTiming
import org.openrndr.extra.style.Transitions
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Element
import org.openrndr.panel.style.*

/** the properties that transition by default with `all` when they are not set: the opacity and the transform */
private val defaults = mapOf<String, Any>("opacity" to 1.0, "transform" to Transform.NONE)

/** the properties that do not transition: those of transitions and animations, and custom properties */
private fun transitions(name: String) =
    !name.startsWith("--") && !name.startsWith("transition") && !name.startsWith("animation")

/** true when values like [value] can be interpolated */
private fun interpolates(value: Any?): Boolean = value is Color.RGBa || value is Double || value is Transform ||
        value is LinearDimension.PX || value is LinearDimension.Percent || value is BoxShadow || value is FlexGrow.Ratio

/**
 * The transitions and animations of the styles of elements.
 *
 * Transitions: when a property of an element with a [org.openrndr.panel.style.transitionDuration] changes, for example
 * by a hover state, its value moves to the new value over the duration, after the
 * [org.openrndr.panel.style.transitionDelay], with the [org.openrndr.panel.style.transitionTimingFunction], as CSS
 * transitions do, see [Transitions]. Colors, numbers, lengths, transforms and shadows transition.
 *
 * Animations: the @keyframes rules of the [org.openrndr.panel.style.animationName]s of elements, see [PanelKeyframes].
 *
 * Runs after the styles are computed, and sets the values of the transitions and animations in the computed styles.
 * @param clock the time in seconds, real time by default
 */
internal class StyleTransitions(private val clock: () -> Double = { System.nanoTime() / 1E9 }) {
    private class State {
        /** the values the styles gave the properties last */
        val targets = mutableMapOf<String, Any>()
        val transitions = Transitions<Any> { _, a, b, t -> mixValues(a, b, t) ?: b }
    }

    private val states = java.util.WeakHashMap<Element, State>()

    /**
     * An animation of an element: the keyframes of [name], with the values of their declarations, since [start];
     * [pausedAt] is the time it was paused at, null while it runs
     */
    private class Animation(val name: String, var start: Double, val frames: List<Pair<Double, Map<String, Any?>>>) {
        var pausedAt: Double? = null
    }

    private val animations = java.util.WeakHashMap<Element, MutableMap<Int, Animation>>()

    /** the elements with transitions or animations that run, they are drawn again */
    var animating: List<Element> = emptyList()
        private set

    /**
     * Applies the transitions and the animations to the styles of [root] and its descendants that were computed again
     * @return true when they change properties that lay out
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
            val (transitioning, transitionsLayout) = transition(element, now)
            // animations override transitions, as in CSS
            val (animating, animationsLayout) = animate(element, now)
            if (transitioning || animating) result.add(element)
            layout = layout || transitionsLayout || animationsLayout
            element.children.forEach(::visit)
        }
        visit(root)
        animating = result
        return layout
    }

    /**
     * Applies the transitions of [element] at [now] to its computed style
     * @return whether transitions run, and whether they change properties that lay out
     */
    private fun transition(element: Element, now: Double): Pair<Boolean, Boolean> {
        val cs = element.computedStyle
        val duration = cs.transitionDuration
        val state = states[element] ?: if (duration > 0.0) State().also { states[element] = it } else return false to false
        val properties = cs.transitionProperty
        val all = "all" in properties
        val names = if (all) (cs.properties.keys + state.targets.keys + defaults.keys).filter(::transitions) else properties
        var layout = false
        for (name in names) {
            val target = cs.getProperty(name)?.value ?: defaults[name] ?: panelProperties()[name]?.initial ?: continue
            if (!interpolates(target)) continue
            val previous = state.targets.put(name, target)
            if (previous != null && !sameValue(previous, target)) {
                val timing = if (duration > 0.0 && (all || name in properties))
                    TransitionTiming(duration, cs.transitionDelay, cs.transitionTimingFunction) else null
                state.transitions.change(name, previous, target, now, timing)
            }
            val value = state.transitions.value(name, now) ?: continue
            cs.setProperty(name, value)
            if (name !in paintProperties) layout = true
        }
        return state.transitions.isRunning to layout
    }

    /**
     * Applies the animations of [element] at [now] to its computed style, the later ones over the earlier ones
     * @return whether animations run, and whether they change properties that lay out
     */
    private fun animate(element: Element, now: Double): Pair<Boolean, Boolean> {
        val cs = element.computedStyle
        val names = cs.animationName
        val running = animations.getOrPut(element) { mutableMapOf() }
        if (names.all { it == "none" }) {
            animations.remove(element)
            return false to false
        }
        fun <T> List<T>.at(i: Int) = this[i % size]
        var anyRunning = false
        var layout = false
        for ((i, name) in names.withIndex()) {
            val keyframes = if (name == "none") null else PanelKeyframes[name]
            if (keyframes.isNullOrEmpty()) {
                running.remove(i)
                continue
            }
            val animation = running[i]?.takeIf { it.name == name } ?: Animation(name, now, keyframes.map { frame ->
                frame.offset to frame.declarations.flatMap { panelProperties().values(it) ?: emptyList() }.toMap()
            }).also { running[i] = it }

            // a paused animation keeps its time
            val paused = cs.animationPlayState.at(i) == AnimationPlayState.PAUSED
            if (paused && animation.pausedAt == null) animation.pausedAt = now
            if (!paused) animation.pausedAt?.let { animation.start += now - it; animation.pausedAt = null }
            val time = animation.pausedAt ?: now

            val duration = cs.animationDuration.at(i)
            val iterations = cs.animationIterationCount.at(i)
            val fill = cs.animationFillMode.at(i)
            val direction = cs.animationDirection.at(i)
            val local = time - animation.start - cs.animationDelay.at(i)
            val active = duration * iterations
            // the progress through the keyframes, null when the animation does not apply
            val progress: Double? = when {
                local < 0.0 -> if (fill == AnimationFillMode.BACKWARDS || fill == AnimationFillMode.BOTH) directed(0.0, 0, direction) else null
                duration <= 0.0 || local >= active -> if (fill == AnimationFillMode.FORWARDS || fill == AnimationFillMode.BOTH) {
                    // the end of the last iteration
                    val last = if (iterations.isInfinite()) 0 else kotlin.math.ceil(iterations).toInt() - 1
                    val fraction = if (iterations.isInfinite()) 1.0 else (iterations - last).coerceIn(0.0, 1.0)
                    directed(fraction, last.coerceAtLeast(0), direction)
                } else null
                else -> {
                    val iteration = kotlin.math.floor(local / duration).toInt()
                    directed(local / duration - iteration, iteration, direction)
                }
            }
            // a paused animation that has not ended is drawn again when it resumes, it does not need frames
            if (!paused && (local < active || (duration > 0.0 && iterations.isInfinite()))) anyRunning = true
            if (progress == null) continue

            // every property of the keyframes, between the keyframes around the progress; the values of the element
            // are the keyframes at 0% and 100% that are not given
            for (property in animation.frames.flatMap { it.second.keys }.toSet()) {
                val base = cs.getProperty(property)?.value ?: panelProperties()[property]?.initial
                val frames = animation.frames.filter { property in it.second }.map { it.first to it.second[property] }.toMutableList()
                if (frames.first().first > 0.0) frames.add(0, 0.0 to base)
                if (frames.last().first < 1.0) frames.add(1.0 to base)
                val after = frames.indexOfFirst { it.first >= progress }.let { if (it < 0) frames.lastIndex else maxOf(it, 1) }
                val (a, from) = frames[after - 1]
                val (b, to) = frames[after]
                val t = if (b > a) cs.animationTimingFunction.at(i).ease(((progress - a) / (b - a)).coerceIn(0.0, 1.0)) else 1.0
                val value = mixValues(from, to, t) ?: if (t < 0.5) from else to
                if (!sameValue(cs.getProperty(property)?.value, value)) {
                    cs.setProperty(property, value)
                    if (property !in paintProperties) layout = true
                }
            }
        }
        return anyRunning to layout
    }

    /** the progress through the keyframes of [fraction] of [iteration], in [direction] */
    private fun directed(fraction: Double, iteration: Int, direction: AnimationDirection): Double = when (direction) {
        AnimationDirection.NORMAL -> fraction
        AnimationDirection.REVERSE -> 1.0 - fraction
        AnimationDirection.ALTERNATE -> if (iteration % 2 == 0) fraction else 1.0 - fraction
        AnimationDirection.ALTERNATE_REVERSE -> if (iteration % 2 == 0) 1.0 - fraction else fraction
    }
}

/**
 * [from] and [to] of a property interpolated at [t]: colors, numbers, lengths of the same unit, flex factors,
 * transforms and shadows; null for values that do not interpolate, which switch half way
 */
internal fun mixValues(from: Any?, to: Any?, t: Double): Any? = when {
    from is Color.RGBa && to is Color.RGBa -> Color.RGBa(from.color.mix(to.color.toLinearity(from.color.linearity), t))
    from is LinearDimension.PX && to is LinearDimension.PX -> LinearDimension.PX(from.value + (to.value - from.value) * t)
    from is LinearDimension.Percent && to is LinearDimension.Percent -> LinearDimension.Percent(from.value + (to.value - from.value) * t)
    from is FlexGrow.Ratio && to is FlexGrow.Ratio -> FlexGrow.Ratio(from.value + (to.value - from.value) * t)
    from is Double && to is Double -> from + (to - from) * t
    from is Transform && to is Transform -> mixTransforms(from, to, t) ?: if (t < 0.5) from else to
    from is BoxShadow && to is BoxShadow -> mixShadows(from, to, t)
    else -> if (t < 0.5) from else to
}

/** shadows interpolated, a list that is shorter is padded with transparent shadows, as in CSS */
private fun mixShadows(from: BoxShadow, to: BoxShadow, t: Double): BoxShadow {
    val transparent = org.openrndr.color.ColorRGBa.TRANSPARENT
    val count = maxOf(from.shadows.size, to.shadows.size)
    fun at(list: List<Shadow>, i: Int, other: List<Shadow>) =
        list.getOrNull(i) ?: other[i].copy(offset = Vector2.ZERO, blur = 0.0, spread = 0.0, color = transparent)
    return BoxShadow(List(count) { i ->
        val a = at(from.shadows, i, to.shadows)
        val b = at(to.shadows, i, from.shadows)
        val ca = a.color ?: org.openrndr.color.ColorRGBa.BLACK
        val cb = b.color ?: org.openrndr.color.ColorRGBa.BLACK
        Shadow(a.offset + (b.offset - a.offset) * t, a.blur + (b.blur - a.blur) * t, a.spread + (b.spread - a.spread) * t,
            ca.mix(cb.toLinearity(ca.linearity), t), if (t < 0.5) a.inset else b.inset)
    })
}
