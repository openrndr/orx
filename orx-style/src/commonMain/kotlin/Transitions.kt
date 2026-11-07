package org.openrndr.extra.style

import kotlin.math.floor
import kotlin.math.max

/** a timing function of CSS, like those of transitions and animations, and the key splines of SMIL */
fun interface TimingFunction {
    /** the progress at [t], from 0 to 1 */
    fun ease(t: Double): Double

    companion object {
        val LINEAR = TimingFunction { it }
        val EASE = cubicBezier(0.25, 0.1, 0.25, 1.0)
        val EASE_IN = cubicBezier(0.42, 0.0, 1.0, 1.0)
        val EASE_OUT = cubicBezier(0.0, 0.0, 0.58, 1.0)
        val EASE_IN_OUT = cubicBezier(0.42, 0.0, 0.58, 1.0)

        /** a cubic Bézier curve from (0, 0) to (1, 1) with the control points ([x1], [y1]) and ([x2], [y2]) */
        fun cubicBezier(x1: Double, y1: Double, x2: Double, y2: Double) = TimingFunction { t ->
            if (t <= 0.0 || t >= 1.0) return@TimingFunction t
            fun x(u: Double) = 3.0 * (1 - u) * (1 - u) * u * x1 + 3.0 * (1 - u) * u * u * x2 + u * u * u
            fun y(u: Double) = 3.0 * (1 - u) * (1 - u) * u * y1 + 3.0 * (1 - u) * u * u * y2 + u * u * u
            // the parameter of the curve at t, by bisection
            var lo = 0.0
            var hi = 1.0
            repeat(40) {
                val mid = (lo + hi) / 2.0
                if (x(mid) < t) lo = mid else hi = mid
            }
            y((lo + hi) / 2.0)
        }

        /** steps of CSS: [count] steps, jumping at their start, their end, both or neither */
        fun steps(count: Int, position: String = "jump-end") = TimingFunction { t ->
            val n = max(count, 1)
            val jumps = when (position) {
                "jump-start", "start" -> 0
                "jump-none" -> -1
                "jump-both" -> 1
                else -> 0
            }
            var step = floor(t * n)
            if (position == "jump-start" || position == "start" || position == "jump-both") step += 1.0
            if (t >= 0.0 && step < 0.0) step = 0.0
            val total = n + jumps
            (step / total).coerceIn(0.0, 1.0)
        }

        /** true when [value] is a timing function of CSS */
        fun isCss(value: String): Boolean {
            val v = value.trim()
            return v in keywords || v.startsWith("cubic-bezier(") || v.startsWith("steps(")
        }

        private val keywords = setOf("linear", "ease", "ease-in", "ease-out", "ease-in-out", "step-start", "step-end")

        /** the timing function of CSS [value], linear when it is not known */
        fun css(value: String): TimingFunction {
            val v = value.trim()
            return when {
                v == "linear" -> LINEAR
                v == "ease" -> EASE
                v == "ease-in" -> EASE_IN
                v == "ease-out" -> EASE_OUT
                v == "ease-in-out" -> EASE_IN_OUT
                v == "step-start" -> steps(1, "jump-start")
                v == "step-end" -> steps(1, "jump-end")
                v.startsWith("cubic-bezier(") -> numbers(v).takeIf { it.size == 4 }?.let { cubicBezier(it[0], it[1], it[2], it[3]) } ?: LINEAR
                v.startsWith("steps(") -> {
                    val arguments = v.substringAfter('(').substringBefore(')').split(',').map { it.trim() }
                    steps(arguments[0].toIntOrNull() ?: 1, arguments.getOrElse(1) { "jump-end" })
                }
                else -> LINEAR
            }
        }

        private fun numbers(v: String) = Regex("-?[0-9.]+(?:e-?[0-9]+)?").findAll(v).mapNotNull { it.value.toDoubleOrNull() }.toList()
    }
}

/**
 * A time of CSS or a clock value of SMIL, in seconds: 2s, 500ms, 1.5, 01:30, 00:01:30.5, 2min, 1h; null when it is
 * not a time
 */
fun parseCssTime(value: String): Double? {
    val v = value.trim()
    if (v.contains(':')) {
        val parts = v.split(':').map { it.toDoubleOrNull() ?: return null }
        return parts.fold(0.0) { acc, p -> acc * 60.0 + p }
    }
    return when {
        v.endsWith("ms") -> v.dropLast(2).toDoubleOrNull()?.div(1000.0)
        v.endsWith("min") -> v.dropLast(3).toDoubleOrNull()?.times(60.0)
        v.endsWith("h") -> v.dropLast(1).toDoubleOrNull()?.times(3600.0)
        v.endsWith("s") -> v.dropLast(1).toDoubleOrNull()
        else -> v.toDoubleOrNull()
    }
}

/**
 * How a property transitions: over [duration] seconds, after [delay] seconds, with [timingFunction]
 */
data class TransitionTiming(
    val duration: Double,
    val delay: Double = 0.0,
    val timingFunction: TimingFunction = TimingFunction.EASE
) {
    companion object {
        /**
         * The timing of the transitions of [property] by the CSS declarations that [declaration] gives by their names:
         * the `transition` shorthand, overridden by the `transition-property`, `transition-duration`,
         * `transition-delay` and `transition-timing-function` longhands. Null when the property does not transition.
         */
        fun css(property: String, declaration: (String) -> String?): TransitionTiming? {
            val parts = mutableListOf<Pair<String, TransitionTiming>>()
            declaration("transition")?.let(::splitCssList)?.forEach { part ->
                var name = "all"
                var duration = 0.0
                var delay = 0.0
                var timing = "ease"
                var times = 0
                for (token in Regex("cubic-bezier\\([^)]*\\)|steps\\([^)]*\\)|\\S+").findAll(part.trim()).map { it.value }) {
                    val time = if (token.endsWith("s")) parseCssTime(token) else null
                    when {
                        time != null -> {
                            if (times == 0) duration = time else delay = time; times++
                        }
                        TimingFunction.isCss(token) -> timing = token
                        else -> name = token
                    }
                }
                parts.add(name to TransitionTiming(duration, delay, TimingFunction.css(timing)))
            }
            // longhands override the shorthand
            val names = declaration("transition-property")?.let(::splitCssList)?.map { it.trim() }
            if (names != null) {
                fun list(name: String) = declaration(name)?.let(::splitCssList)?.map { it.trim() }
                val durations = list("transition-duration") ?: listOf("0s")
                val delays = list("transition-delay") ?: listOf("0s")
                val timings = list("transition-timing-function") ?: listOf("ease")
                parts.clear()
                names.forEachIndexed { i, n ->
                    parts.add(
                        n to TransitionTiming(
                            parseCssTime(durations[i % durations.size]) ?: 0.0,
                            parseCssTime(delays[i % delays.size]) ?: 0.0,
                            TimingFunction.css(timings[i % timings.size])
                        )
                    )
                }
            }
            return parts.lastOrNull { it.first == property || it.first == "all" }?.second?.takeIf { it.duration > 0.0 }
        }
    }
}

/**
 * A transition of [property] from [from] to [to] that starts at [start], in seconds
 */
class Transition<V>(val property: String, val from: V, val to: V, val start: Double, val timing: TransitionTiming) {
    /** the progress at [time], before the timing function: below 0 during the delay, 1 or more once it ended */
    fun progress(time: Double): Double = (time - start - timing.delay) / timing.duration

    /** true once the delay is over, when the transition has started */
    var started = false
        internal set
}

/**
 * The transitions of the properties of one element, of values of type [V] that [interpolate] interpolates: a change
 * of a property with a [TransitionTiming] transitions from the value that is shown, also when another transition of
 * the property runs, to the new value. Transitions of CSS.
 * @param interpolate the value of `property` between `a` and `b` at `t`, from 0 to 1
 */
class Transitions<V>(private val interpolate: (property: String, a: V, b: V, t: Double) -> V) {
    private val running = mutableMapOf<String, Transition<V>>()

    /** the transitions that run */
    val transitions: Collection<Transition<V>> get() = running.values

    /** true when transitions run */
    val isRunning: Boolean get() = running.isNotEmpty()

    /** called when a transition starts, after its delay */
    var started: (Transition<V>) -> Unit = {}

    /** called when a transition ends */
    var ended: (Transition<V>) -> Unit = {}

    /** called when a transition that did not end is replaced */
    var canceled: (Transition<V>) -> Unit = {}

    /**
     * The value of [property] at [time], of the transition that runs; null when none runs. Starts and ends the
     * transition, which is removed when it ended.
     */
    fun value(property: String, time: Double): V? {
        val transition = running[property] ?: return null
        val t = transition.progress(time)
        if (t > 0.0 && !transition.started) {
            transition.started = true
            started(transition)
        }
        return when {
            t >= 1.0 -> {
                running.remove(property)
                ended(transition)
                null
            }
            t <= 0.0 -> transition.from
            else -> interpolate(property, transition.from, transition.to, transition.timing.timingFunction.ease(t))
        }
    }

    /** the value of [property] that is shown at [time], without starting or ending transitions; null when none runs */
    fun shown(property: String, time: Double): V? {
        val transition = running[property] ?: return null
        val t = transition.progress(time)
        return when {
            t >= 1.0 -> null
            t <= 0.0 -> transition.from
            else -> interpolate(property, transition.from, transition.to, transition.timing.timingFunction.ease(t))
        }
    }

    /**
     * [property] changes from [from] to [to] at [time]: a transition with [timing] starts from the value that is
     * shown, [from] when no transition runs. A transition that runs is canceled, also when [timing] is null.
     */
    fun change(property: String, from: V, to: V, time: Double, timing: TransitionTiming?) {
        val shown = shown(property, time)
        running.remove(property)?.let { if (it.progress(time) < 1.0) canceled(it) }
        if (timing != null && timing.duration > 0.0) {
            running[property] = Transition(property, shown ?: from, to, time, timing)
        }
    }

    /** removes the transition of [property], without events */
    fun remove(property: String) {
        running.remove(property)
    }
}
