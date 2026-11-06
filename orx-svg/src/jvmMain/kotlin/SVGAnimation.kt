package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Element
import org.openrndr.extra.composition.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/*
 * Animation of SVG documents: SMIL animation elements, CSS animations with @keyframes, rules for :hover and :active
 * and CSS transitions. Values are text, as they are written in documents: they are interpolated as colors, as lists
 * of numbers, or switched discretely, and applied to the nodes of elements with the parsers that load documents.
 */

/** a clock value of SMIL or a CSS time: 2s, 500ms, 1.5, 01:30, 00:01:30.5, 2min, 1h; null when it is not valid */
internal fun clockValue(value: String): Double? {
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

/** a timing function, like the easing functions of CSS and the key splines of SMIL */
internal fun interface Easing {
    fun ease(t: Double): Double

    companion object {
        val LINEAR = Easing { it }

        /** a cubic Bézier curve from (0, 0) to (1, 1) with the control points ([x1], [y1]) and ([x2], [y2]) */
        fun cubicBezier(x1: Double, y1: Double, x2: Double, y2: Double) = Easing { t ->
            if (t <= 0.0 || t >= 1.0) return@Easing t
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
        fun steps(count: Int, position: String) = Easing { t ->
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

        /** the timing function of CSS named [value], linear when it is not known */
        fun css(value: String): Easing {
            val v = value.trim()
            return when {
                v == "linear" -> LINEAR
                v == "ease" -> cubicBezier(0.25, 0.1, 0.25, 1.0)
                v == "ease-in" -> cubicBezier(0.42, 0.0, 1.0, 1.0)
                v == "ease-out" -> cubicBezier(0.0, 0.0, 0.58, 1.0)
                v == "ease-in-out" -> cubicBezier(0.42, 0.0, 0.58, 1.0)
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
 * The interpolation of values given as text: colors in sRGB, lists of numbers with the same text around them, like
 * lengths, transform lists and path data, and other values discretely
 */
internal object ValueInterpolation {
    private val number = Regex("[-+]?(?:[0-9]*\\.[0-9]+|[0-9]+\\.?)(?:[eE][-+]?[0-9]+)?")

    private fun color(value: String): org.openrndr.color.ColorRGBa? =
        (SVGParse.color(value) as? Paint.RGB)?.value?.toSRGB()

    private fun format(v: Double): String {
        val rounded = kotlin.math.round(v * 1E6) / 1E6
        return if (rounded == kotlin.math.round(rounded)) rounded.toLong().toString() else rounded.toString()
    }

    /** the numbers of [value] and the text around them, null when it has no numbers */
    private fun split(value: String): Pair<List<String>, List<Double>>? {
        val matches = number.findAll(value).toList()
        if (matches.isEmpty()) return null
        val text = mutableListOf<String>()
        var position = 0
        for (m in matches) {
            text.add(value.substring(position, m.range.first))
            position = m.range.last + 1
        }
        text.add(value.substring(position))
        return Pair(text, matches.map { it.value.toDouble() })
    }

    private fun join(text: List<String>, numbers: List<Double>): String = buildString {
        for (i in numbers.indices) {
            append(text[i]); append(format(numbers[i]))
        }
        append(text.last())
    }

    private val transformList = Regex("^\\s*(?:[a-zA-Z0-9]+\\([^)]*\\)\\s*)+$")

    /**
     * The identity transform list with the functions of [list], like CSS interpolates a transform from none: zeros
     * for translations, rotations and skews, ones for scales
     */
    private fun identity(list: String): String = Regex("([a-zA-Z0-9]+)\\(([^)]*)\\)").findAll(list).joinToString(" ") { m ->
        val name = m.groupValues[1]
        val count = m.groupValues[2].split(Regex("[\\s,]+")).count { it.isNotBlank() }
        when {
            name.startsWith("scale") -> "$name(${List(count) { "1" }.joinToString(" ")})"
            name == "matrix" -> "matrix(1 0 0 1 0 0)"
            // the center of a rotation is kept
            name == "rotate" && count == 3 -> {
                val n = m.groupValues[2].split(Regex("[\\s,]+")).filter { it.isNotBlank() }
                "rotate(0 ${n[1]} ${n[2]})"
            }
            else -> "$name(${List(count) { "0" }.joinToString(" ")})"
        }
    }

    /** [a] and [b] mixed by [t]; values that are not interpolated switch at [discreteAt] */
    fun interpolate(a: String, b: String, t: Double, discreteAt: Double = 0.5): String {
        // a transform from or to none is interpolated from or to the identity of the other
        val noneA = a.isBlank() || a.trim() == "none"
        val noneB = b.isBlank() || b.trim() == "none"
        if (noneA && !noneB && transformList.matches(b)) return interpolate(identity(b), b, t, discreteAt)
        if (noneB && !noneA && transformList.matches(a)) return interpolate(a, identity(a), t, discreteAt)
        if (t <= 0.0 && discreteAt > 0.0) return a
        val ca = color(a)
        val cb = color(b)
        if (ca != null && cb != null && !(split(a) != null && split(b) != null && sameNumbers(a, b) && ca == cb)) {
            // in sRGB with premultiplied alpha, as CSS interpolates colors
            val alpha = ca.alpha + (cb.alpha - ca.alpha) * t
            fun channel(x: Double, y: Double) = if (alpha <= 0.0) 0.0 else (x * ca.alpha + (y * cb.alpha - x * ca.alpha) * t) / alpha
            val r = (channel(ca.r, cb.r) * 255.0).coerceIn(0.0, 255.0)
            val g = (channel(ca.g, cb.g) * 255.0).coerceIn(0.0, 255.0)
            val bl = (channel(ca.b, cb.b) * 255.0).coerceIn(0.0, 255.0)
            return "rgba(${format(r)}, ${format(g)}, ${format(bl)}, ${format(alpha.coerceIn(0.0, 1.0))})"
        }
        val sa = split(a)
        val sb = split(b)
        if (sa != null && sb != null && sa.second.size == sb.second.size && sa.first.map { it.trim() } == sb.first.map { it.trim() }) {
            return join(sa.first, sa.second.zip(sb.second) { x, y -> x + (y - x) * t })
        }
        return if (t < discreteAt) a else b
    }

    private fun sameNumbers(a: String, b: String) = split(a)?.second == split(b)?.second

    /** [a] plus [b] when they are lists of numbers with the same text around them, null otherwise */
    fun add(a: String, b: String): String? {
        val ca = color(a)
        val cb = color(b)
        if (ca != null && cb != null && split(a) == null) {
            return "rgb(${format((ca.r + cb.r).coerceIn(0.0, 1.0) * 255.0)}, ${format((ca.g + cb.g).coerceIn(0.0, 1.0) * 255.0)}, ${format((ca.b + cb.b).coerceIn(0.0, 1.0) * 255.0)})"
        }
        val sa = split(a) ?: return null
        val sb = split(b) ?: return null
        if (sa.second.size != sb.second.size) return null
        return join(sb.first, sa.second.zip(sb.second) { x, y -> x + y })
    }

    /** the numbers of [a] multiplied by [k], null when it has none */
    fun scale(a: String, k: Double): String? {
        val sa = split(a) ?: return null
        return join(sa.first, sa.second.map { it * k })
    }

    /** a distance between [a] and [b], the distance of their numbers, for paced animation */
    fun distance(a: String, b: String): Double {
        val ca = color(a)
        val cb = color(b)
        if (ca != null && cb != null && split(a) == null) {
            return sqrt((ca.r - cb.r) * (ca.r - cb.r) + (ca.g - cb.g) * (ca.g - cb.g) + (ca.b - cb.b) * (ca.b - cb.b)) * 255.0
        }
        val na = split(a)?.second ?: return 1.0
        val nb = split(b)?.second ?: return 1.0
        if (na.size != nb.size) return 1.0
        return sqrt(na.zip(nb).sumOf { (x, y) -> (x - y) * (x - y) })
    }
}

/**
 * The nodes of an element that animations change: its attributes and properties are set as text and applied to the
 * nodes with the parsers that load documents. The values of the nodes as they are loaded are restored before every
 * update.
 */
internal class AnimationTarget(val element: Element, val nodes: List<CompositionNode>) {
    private val baseStyles = nodes.map { HashMap(it.style.properties) }
    private val baseShapes = nodes.map { (it as? ShapeNode)?.shape }

    /** the geometry attributes of shapes, they change the shape */
    private val geometry = setOf("x", "y", "width", "height", "rx", "ry", "cx", "cy", "r", "x1", "y1", "x2", "y2", "points", "d")

    /** the attributes and properties set since the last restore */
    private val values = mutableMapOf<String, String>()

    fun restore() {
        values.clear()
        for ((i, node) in nodes.withIndex()) {
            node.style.properties.clear()
            node.style.properties.putAll(baseStyles[i])
            baseShapes[i]?.let { (node as ShapeNode).shape = it }
        }
    }

    /** the value of [name] as it is specified for the element, or inherited, or its initial value */
    fun baseValue(name: String): String {
        values[name]?.let { return it }
        fun specified(e: Element): String? =
            SVGStyleSheet.declarations(e.attr("style")).lastOrNull { it.first == name }?.second
                ?: e.attr(name).takeIf { e.hasAttr(name) }
        specified(element)?.let { return it }
        val key = AttributeOrPropertyKey.entries.firstOrNull { it.toString() == name }
        if (key != null && isInheritedProperty(key)) {
            element.parents().firstNotNullOfOrNull { specified(it) }?.let { return it }
        }
        if (name == "transform") return ""
        if (name in geometry) return "0"
        return key?.let { initialValue(it)?.toString() } ?: ""
    }

    /** sets [name] to [value] on the nodes of the element */
    fun set(name: String, value: String) {
        values[name] = value
        when {
            name == "transform" -> applyTransform()
            name == "motion" -> applyTransform()
            name in geometry -> {
                val clone = element.clone()
                values.filterKeys { it in geometry }.forEach { (k, v) -> clone.attr(k, v) }
                val shape = SVGPath(clone).shape()
                nodes.forEach { (it as? ShapeNode)?.shape = shape }
            }
            else -> nodes.forEach { node ->
                PropertySetter(element, node).styleProperty(name, value)
            }
        }
    }

    /** the transform of the nodes: the motion of animateMotion, then the transform attribute or property */
    private fun applyTransform() {
        val transform = values["transform"] ?: baseValue("transform")
        val motion = values["motion"] ?: ""
        val setter = PropertySetter(element, null)
        setter.style.transform = SVGParse.transform(listOf(motion, transform).filter { it.isNotBlank() }.joinToString(" "))
        val matrix = setter.effectiveTransform()
        nodes.forEach { it.transform = matrix }
    }
}

/** sets the properties of the style of a node, and computes the transform of an element around its origin */
private class PropertySetter(element: Element, node: CompositionNode?) : SVGElement(element) {
    init {
        if (node != null) style = node.style
        // the origin of the transform of the element
        val origin = SVGStyleSheet.declarations(element.attr("style")).lastOrNull { it.first == "transform-origin" }?.second
            ?: element.attr("transform-origin").takeIf { it.isNotBlank() }
        transformOrigin = origin
    }

    override fun handleAttribute(attribute: Attribute) {}
}

/** when an animation begins or ends */
internal sealed interface TimeCondition {
    class Offset(val time: Double) : TimeCondition
    /** an event on the element with [id], or on the target when it is null */
    class Event(val id: String?, val event: String, val offset: Double) : TimeCondition
    /** the begin or end of the animation with [id] */
    class Sync(val id: String, val end: Boolean, val offset: Double) : TimeCondition
    object Indefinite : TimeCondition

    companion object {
        /** the conditions of a begin or end attribute, separated by semicolons */
        fun parse(value: String?): List<TimeCondition> {
            if (value == null) return emptyList()
            return value.split(';').mapNotNull { part ->
                val v = part.trim()
                if (v.isEmpty()) return@mapNotNull null
                if (v == "indefinite") return@mapNotNull Indefinite
                clockValue(v)?.let { return@mapNotNull Offset(it) }
                // an id, a dot and an event or begin or end, with an offset
                val match = Regex("^(?:([A-Za-z_][\\w-]*)\\.)?([A-Za-z]+)(?:\\s*([+-])\\s*(.+))?$").matchEntire(v)
                    ?: return@mapNotNull null.also { logger.warn { "unsupported timing '$v'" } }
                val offset = match.groupValues[4].takeIf { it.isNotEmpty() }?.let { clockValue(it) }?.let {
                    if (match.groupValues[3] == "-") -it else it
                } ?: 0.0
                val id = match.groupValues[1].takeIf { it.isNotEmpty() }
                when (val name = match.groupValues[2]) {
                    "begin", "end" -> if (id != null) Sync(id, name == "end", offset) else null
                    else -> Event(id, name, offset)
                }
            }
        }
    }
}

/**
 * A SMIL animation element: animate, set, animateTransform or animateMotion
 */
internal class SmilAnimation(
    val element: Element,
    val target: AnimationTarget,
    val attribute: String,
    val values: List<String>,
    val keyTimes: List<Double>?,
    val keySplines: List<Easing>?,
    val calcMode: String,
    val duration: Double?,
    val repeatCount: Double?,
    val repeatDuration: Double?,
    val freeze: Boolean,
    val additive: Boolean,
    val accumulate: Boolean,
    val toAnimation: Boolean,
    val begins: List<TimeCondition>,
    val ends: List<TimeCondition>,
    /** for animateTransform: the type of the transform */
    val transformType: String?,
    /** for animateMotion: the path and the rotation, auto, auto-reverse or an angle */
    val motionPath: ShapeContour?,
    val motionRotate: String?,
    val keyPoints: List<Double>?
) {
    val id: String = element.id()
    /** times when the animation begins and ends that events and syncbases resolved */
    val beginInstances = mutableListOf<Double>()
    val endInstances = mutableListOf<Double>()

    /** the interval that is active or was last active at the last update */
    var intervalBegin: Double? = null
    var intervalEnd: Double? = null

    /** the length of the active duration from [begin] */
    fun activeDuration(begin: Double): Double {
        val simple = duration
        val repeat = when {
            repeatCount != null && simple != null -> repeatCount * simple
            repeatDuration != null -> Double.POSITIVE_INFINITY
            else -> simple ?: Double.POSITIVE_INFINITY
        }
        var active = if (repeatDuration != null) min(repeat, repeatDuration) else repeat
        val end = endInstances.filter { it > begin }.minOrNull()
        if (end != null) active = min(active, end - begin)
        // an indefinite duration with an end but no repeat lasts until the end
        return active
    }

    /** the value at [fraction] of the simple duration, in iteration [iteration] */
    fun value(fraction: Double, iteration: Int): String? {
        val vs = if (toAnimation) listOf(target.baseValue(attribute), values.last()) else values
        if (vs.isEmpty()) return null
        var value = when {
            vs.size == 1 -> vs[0]
            calcMode == "discrete" -> {
                val times = keyTimes?.takeIf { it.size == vs.size } ?: vs.indices.map { it.toDouble() / vs.size }
                vs[times.indexOfLast { it <= fraction + 1E-12 }.coerceAtLeast(0)]
            }
            else -> {
                val times = keyTimes?.takeIf { it.size == vs.size } ?: if (calcMode == "paced") {
                    val distances = vs.zipWithNext { a, b -> ValueInterpolation.distance(a, b) }
                    val total = distances.sum()
                    if (total <= 0.0) vs.indices.map { it.toDouble() / (vs.size - 1) }
                    else distances.runningFold(0.0) { acc, d -> acc + d }.map { it / total }
                } else vs.indices.map { it.toDouble() / (vs.size - 1) }
                val i = times.indexOfLast { it <= fraction }.coerceIn(0, vs.size - 2)
                val span = times[i + 1] - times[i]
                var local = if (span <= 0.0) 1.0 else ((fraction - times[i]) / span).coerceIn(0.0, 1.0)
                if (calcMode == "spline") keySplines?.getOrNull(i)?.let { local = it.ease(local) }
                ValueInterpolation.interpolate(vs[i], vs[i + 1], local, discreteAt = 1.0)
            }
        }
        if (accumulate && iteration > 0 && !toAnimation) {
            ValueInterpolation.scale(vs.last(), iteration.toDouble())?.let { last -> ValueInterpolation.add(last, value)?.let { value = it } }
        }
        return value
    }
}

/** a CSS animation of an element: @keyframes with the properties of the animation property */
internal class CssAnimation(
    val target: AnimationTarget,
    val keyframes: List<SVGStyleSheet.Keyframe>,
    val duration: Double,
    val easing: Easing,
    val delay: Double,
    val iterations: Double,
    val direction: String,
    val fillMode: String,
    val paused: Boolean
) {
    /** the values of the properties of the keyframes at [time], null when the animation has no effect */
    fun values(time: Double): Map<String, String>? {
        if (duration <= 0.0 && iterations > 0.0) return null
        val elapsed = time - delay
        val total = duration * iterations
        val progress: Double
        val iteration: Int
        when {
            elapsed < 0.0 -> {
                if (fillMode != "backwards" && fillMode != "both") return null
                progress = 0.0; iteration = 0
            }
            elapsed >= total -> {
                if (fillMode != "forwards" && fillMode != "both") return null
                // the end of the last iteration
                val iterationsDone = iterations
                iteration = (if (iterationsDone == floor(iterationsDone)) iterationsDone - 1 else floor(iterationsDone)).toInt().coerceAtLeast(0)
                progress = if (iterationsDone == floor(iterationsDone)) 1.0 else iterationsDone - floor(iterationsDone)
            }
            else -> {
                iteration = floor(elapsed / duration).toInt()
                progress = elapsed / duration - iteration
            }
        }
        val reversed = when (direction) {
            "reverse" -> true
            "alternate" -> iteration % 2 == 1
            "alternate-reverse" -> iteration % 2 == 0
            else -> false
        }
        val p = if (reversed) 1.0 - progress else progress
        val properties = keyframes.flatMap { frame -> frame.declarations.map { it.first } }
            .filter { it != "animation-timing-function" }.distinct()
        return properties.associateWith { property ->
            // the keyframes with the property, the base value where the first or the last is missing
            val frames = keyframes.filter { frame -> frame.declarations.any { it.first == property } }
                .map { frame -> Triple(frame.offset, frame.declarations.last { it.first == property }.second,
                    frame.declarations.lastOrNull { it.first == "animation-timing-function" }?.second?.let { Easing.css(it) }) }
                .toMutableList()
            val base = target.baseValue(property)
            if (frames.first().first > 0.0) frames.add(0, Triple(0.0, base, null))
            if (frames.last().first < 1.0) frames.add(Triple(1.0, base, null))
            val i = frames.indexOfLast { it.first <= p }.coerceIn(0, frames.size - 2)
            val (o1, v1, e1) = frames[i]
            val (o2, v2, _) = frames[i + 1]
            val local = if (o2 <= o1) 1.0 else ((p - o1) / (o2 - o1)).coerceIn(0.0, 1.0)
            ValueInterpolation.interpolate(v1, v2, (e1 ?: easing).ease(local))
        }
    }
}

/** the transition settings of an element for a property: its duration, delay and timing function */
internal class TransitionSettings(val duration: Double, val delay: Double, val easing: Easing)

/** a transition of a property of an element that is running */
private class RunningTransition(val from: String, val to: String, val start: Double, val settings: TransitionSettings)

/**
 * The animation of a composition that is loaded from an SVG document: its SMIL animation elements, CSS animations,
 * rules for :hover and :active, and CSS transitions. [update] sets the values of the composition at a time;
 * [listenTo] connects it to the pointer, for events that SMIL animations begin or end with and for pointer states.
 *
 * ```
 * val composition = loadSVG("animated.svg")
 * composition.animation?.listenTo(CompositionInteraction(composition).apply { listenTo(mouse) })
 * extend {
 *     composition.evaluate(seconds)
 *     drawer.composition(composition)
 * }
 * ```
 */
class SVGAnimation internal constructor(document: SVGDocument) {
    private val targets = java.util.IdentityHashMap<Element, AnimationTarget>()
    private val elementsByNode = java.util.IdentityHashMap<CompositionNode, Element>()
    private val nodesByElement = document.nodesByElement
    private val smil = mutableListOf<SmilAnimation>()
    private val css = mutableListOf<CssAnimation>()
    private val stateRules = document.dynamic.stateRules

    /** the elements in each pointer state */
    private val hovered = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Element, Boolean>())
    private val active = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Element, Boolean>())

    /** the values that the rules for pointer states and transitions last set, by element and property */
    private val targetValues = java.util.IdentityHashMap<Element, MutableMap<String, String>>()
    private val displayed = java.util.IdentityHashMap<Element, MutableMap<String, String>>()
    private val transitions = java.util.IdentityHashMap<Element, MutableMap<String, RunningTransition>>()

    /** the time of the last update */
    var time = 0.0
        private set

    private fun target(element: Element): AnimationTarget? {
        targets[element]?.let { return it }
        val nodes = nodesByElement[element] ?: return null
        return AnimationTarget(element, nodes).also { targets[element] = it }
    }

    init {
        for ((element, nodes) in nodesByElement) for (node in nodes) elementsByNode[node] = element
        val root = document.rootElement
        val ids = root.allElements.filter { it.id().isNotEmpty() }.associateBy { it.id() }
        for (element in root.allElements) {
            when (element.tagName()) {
                "animate", "set", "animateTransform", "animateMotion", "animateColor" -> smilAnimation(element, ids, document)?.let { smil.add(it) }
            }
        }
        // CSS animations and the targets of rules for pointer states
        for (element in root.allElements) {
            val declarations = SVGStyleSheet.declarations(element.attr("style"))
            cssAnimations(element, declarations, document.dynamic.keyframes)
        }
        for (rule in stateRules) target(rule.element)
        // the animations begin at their offsets
        for (a in smil) for (c in a.begins) if (c is TimeCondition.Offset) a.beginInstances.add(c.time)
        for (a in smil) for (c in a.ends) if (c is TimeCondition.Offset) a.endInstances.add(c.time)
        // without a begin attribute an animation begins at 0
        for (a in smil) if (a.begins.isEmpty()) a.beginInstances.add(0.0)
    }

    /** true when the document has animations, transitions or rules for pointer states */
    val animated: Boolean get() = smil.isNotEmpty() || css.isNotEmpty() || stateRules.isNotEmpty()

    private fun smilAnimation(element: Element, ids: Map<String, Element>, document: SVGDocument): SmilAnimation? {
        val href = listOf("href", "xlink:href").firstNotNullOfOrNull { element.attr(it).trim().takeIf { h -> h.isNotEmpty() } }
        val targetElement = href?.let { ids[it.removePrefix("#")] } ?: element.parent() ?: return null
        val target = target(targetElement) ?: return null.also { logger.warn { "animation of '${targetElement.tagName()}' is not supported" } }
        val tag = element.tagName()
        val attribute = when (tag) {
            "animateTransform" -> "transform"
            "animateMotion" -> "motion"
            else -> element.attr("attributeName").trim().takeIf { it.isNotEmpty() } ?: return null
        }
        fun attr(name: String) = element.attr(name).trim().takeIf { it.isNotEmpty() }
        val transformType = if (tag == "animateTransform") attr("type") ?: "translate" else null
        fun wrap(v: String) = if (transformType != null) "$transformType(${v.trim()})" else v.trim()

        var values = attr("values")?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }?.map(::wrap)
        var toAnimation = false
        if (values == null) {
            val from = attr("from")?.let(::wrap)
            val to = attr("to")?.let(::wrap)
            val by = attr("by")?.let(::wrap)
            values = when {
                tag == "set" -> listOfNotNull(to)
                from != null && to != null -> listOf(from, to)
                from != null && by != null -> listOf(from, ValueInterpolation.add(from, by) ?: by)
                to != null -> { toAnimation = true; listOf(to) }
                by != null -> {
                    val base = if (transformType != null) wrap(numbersOf(by).joinToString(" ") { "0" }) else target.baseValue(attribute)
                    listOf(base, ValueInterpolation.add(base, by) ?: by)
                }
                else -> emptyList()
            }
        }

        // the path of animateMotion: the path attribute, an mpath element or the values as points
        var motionPath: ShapeContour? = null
        if (tag == "animateMotion") {
            val mpath = element.children().firstOrNull { it.tagName() == "mpath" }
                ?.let { m -> listOf("href", "xlink:href").firstNotNullOfOrNull { m.attr(it).trim().takeIf { h -> h.isNotEmpty() } } }
                ?.let { ids[it.removePrefix("#")]?.attr("d") }
            val d = attr("path") ?: mpath
            motionPath = if (d != null) svgShape(d).contours.firstOrNull() else {
                val points = values.mapNotNull { v -> numbersOf(v).takeIf { it.size >= 2 }?.let { Vector2(it[0], it[1]) } }
                if (points.size >= 2) ShapeContour.fromPoints(points, closed = false) else null
            }
            values = listOf("0", "1")
        }

        val calcMode = attr("calcMode") ?: when (tag) {
            "set" -> "discrete"
            "animateMotion" -> "paced"
            else -> "linear"
        }
        val keySplines = attr("keySplines")?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { s ->
            numbersOf(s).takeIf { it.size == 4 }?.let { Easing.cubicBezier(it[0], it[1], it[2], it[3]) } ?: Easing.LINEAR
        }
        val duration = attr("dur")?.let { if (it == "indefinite" || it == "media") null else clockValue(it) }
            ?: if (tag == "set") null else null
        val repeatCount = attr("repeatCount")?.let { if (it == "indefinite") Double.POSITIVE_INFINITY else it.toDoubleOrNull() }
        val repeatDuration = attr("repeatDur")?.let { if (it == "indefinite") Double.POSITIVE_INFINITY else clockValue(it) }
        return SmilAnimation(
            element, target, attribute, values,
            attr("keyTimes")?.split(';')?.mapNotNull { it.trim().toDoubleOrNull() },
            keySplines, calcMode, duration, repeatCount, repeatDuration,
            attr("fill") == "freeze", attr("additive") == "sum", attr("accumulate") == "sum", toAnimation,
            TimeCondition.parse(attr("begin")), TimeCondition.parse(attr("end")),
            transformType, motionPath, attr("rotate"),
            attr("keyPoints")?.split(';')?.mapNotNull { it.trim().toDoubleOrNull() }
        )
    }

    private fun numbersOf(v: String) = Regex("[-+]?(?:[0-9]*\\.[0-9]+|[0-9]+)(?:[eE][-+]?[0-9]+)?").findAll(v).map { it.value.toDouble() }.toList()

    /** the CSS animations of [element], from its animation properties */
    private fun cssAnimations(element: Element, declarations: List<Pair<String, String>>, keyframes: Map<String, List<SVGStyleSheet.Keyframe>>) {
        if (declarations.none { it.first.startsWith("animation") }) return
        fun longhand(name: String) = declarations.lastOrNull { it.first == name }?.second?.split(',')?.map { it.trim() }
        // the shorthand: a name, durations then delays, a timing function, an iteration count, a direction, a fill mode
        val shorthand = declarations.lastOrNull { it.first == "animation" }?.second?.split(',')?.map { it.trim() } ?: emptyList()
        data class Settings(var name: String? = null, var duration: Double = 0.0, var delay: Double = 0.0, var easing: String = "ease",
                            var iterations: Double = 1.0, var direction: String = "normal", var fill: String = "none", var paused: Boolean = false)
        val settings = shorthand.map { part ->
            val s = Settings()
            var times = 0
            for (token in Regex("cubic-bezier\\([^)]*\\)|steps\\([^)]*\\)|\\S+").findAll(part).map { it.value }) {
                val time = if (token.endsWith("s")) clockValue(token) else null
                when {
                    time != null -> { if (times == 0) s.duration = time else s.delay = time; times++ }
                    token == "infinite" -> s.iterations = Double.POSITIVE_INFINITY
                    token.toDoubleOrNull() != null -> s.iterations = token.toDouble()
                    token in setOf("normal", "reverse", "alternate", "alternate-reverse") -> s.direction = token
                    token in setOf("none", "forwards", "backwards", "both") -> s.fill = token
                    token in setOf("running", "paused") -> s.paused = token == "paused"
                    token.startsWith("cubic-bezier") || token.startsWith("steps") || token in setOf("linear", "ease", "ease-in", "ease-out", "ease-in-out", "step-start", "step-end") -> s.easing = token
                    else -> s.name = token
                }
            }
            s
        }.toMutableList()
        val names = longhand("animation-name")
        val count = max(settings.size, names?.size ?: 0)
        while (settings.size < count) settings.add(Settings())
        fun each(name: String, apply: (Settings, String) -> Unit) = longhand(name)?.let { list ->
            settings.forEachIndexed { i, s -> apply(s, list[i % list.size]) }
        }
        each("animation-name") { s, v -> s.name = v }
        each("animation-duration") { s, v -> s.duration = clockValue(v) ?: 0.0 }
        each("animation-delay") { s, v -> s.delay = clockValue(v) ?: 0.0 }
        each("animation-timing-function") { s, v -> s.easing = v }
        each("animation-iteration-count") { s, v -> s.iterations = if (v == "infinite") Double.POSITIVE_INFINITY else v.toDoubleOrNull() ?: 1.0 }
        each("animation-direction") { s, v -> s.direction = v }
        each("animation-fill-mode") { s, v -> s.fill = v }
        each("animation-play-state") { s, v -> s.paused = v == "paused" }
        val target = target(element) ?: return
        for (s in settings) {
            val frames = keyframes[s.name ?: continue] ?: continue.also { logger.warn { "@keyframes ${s.name} is not defined" } }
            css.add(CssAnimation(target, frames, s.duration, Easing.css(s.easing), s.delay, s.iterations, s.direction, s.fill, s.paused))
        }
    }

    /** the transition settings of [declarations] for [property], null when it does not transition */
    private fun transitionSettings(declarations: List<Pair<String, String>>, property: String): TransitionSettings? {
        val parts = mutableListOf<Triple<String, TransitionSettings, Int>>()
        declarations.lastOrNull { it.first == "transition" }?.second?.split(',')?.forEachIndexed { i, part ->
            var name = "all"
            var duration = 0.0
            var delay = 0.0
            var easing = "ease"
            var times = 0
            for (token in Regex("cubic-bezier\\([^)]*\\)|steps\\([^)]*\\)|\\S+").findAll(part.trim()).map { it.value }) {
                val time = if (token.endsWith("s")) clockValue(token) else null
                when {
                    time != null -> { if (times == 0) duration = time else delay = time; times++ }
                    token.startsWith("cubic-bezier") || token.startsWith("steps") || token in setOf("linear", "ease", "ease-in", "ease-out", "ease-in-out", "step-start", "step-end") -> easing = token
                    else -> name = token
                }
            }
            parts.add(Triple(name, TransitionSettings(duration, delay, Easing.css(easing)), i))
        }
        // longhands override the shorthand
        val names = declarations.lastOrNull { it.first == "transition-property" }?.second?.split(',')?.map { it.trim() }
        if (names != null) {
            fun list(name: String) = declarations.lastOrNull { it.first == name }?.second?.split(',')?.map { it.trim() }
            val durations = list("transition-duration") ?: listOf("0s")
            val delays = list("transition-delay") ?: listOf("0s")
            val easings = list("transition-timing-function") ?: listOf("ease")
            parts.clear()
            names.forEachIndexed { i, n ->
                parts.add(Triple(n, TransitionSettings(clockValue(durations[i % durations.size]) ?: 0.0,
                    clockValue(delays[i % delays.size]) ?: 0.0, Easing.css(easings[i % easings.size])), i))
            }
        }
        return parts.lastOrNull { it.first == property || it.first == "all" }?.second?.takeIf { it.duration > 0.0 }
    }

    /** the declarations of [element] with the rules for pointer states that apply now */
    private fun stateDeclarations(element: Element): List<Pair<String, String>> {
        val base = SVGStyleSheet.declarations(element.attr("style"))
        val rules = stateRules.filter { it.element === element && when (it.state) {
            SVGStyleSheet.State.HOVER -> it.condition in hovered
            SVGStyleSheet.State.ACTIVE -> it.condition in active
        } }.sortedWith(compareBy({ it.specificity }, { it.order }))
        return base + rules.flatMap { it.declarations }
    }

    /** the elements of the nodes from [node] up, the target of an event and the elements it bubbles to */
    private fun elementPath(node: CompositionNode): List<Element> =
        generateSequence(node) { it.parent }.mapNotNull { elementsByNode[it] }.distinct().toList()

    /** an [event] on [element], at the time of the last update: animations that begin or end with it get a time */
    fun dispatch(element: Element, event: String) {
        val names = when (event) {
            "mouseenter" -> setOf("mouseenter", "mouseover")
            "mouseleave" -> setOf("mouseleave", "mouseout")
            else -> setOf(event)
        }
        for (a in smil) {
            fun matches(c: TimeCondition) = c is TimeCondition.Event && c.event in names &&
                    (if (c.id == null) a.target.element === element else element.id() == c.id)
            for (c in a.begins) if (matches(c)) a.beginInstances.add(time + (c as TimeCondition.Event).offset)
            for (c in a.ends) if (matches(c)) a.endInstances.add(time + (c as TimeCondition.Event).offset)
        }
    }

    /**
     * Listens to the events of [interaction]: the pointer entering and leaving, pressing and clicking nodes begins and
     * ends animations, and puts their elements in the :hover and :active states
     */
    fun listenTo(interaction: CompositionInteraction) {
        interaction.entered.listen { e ->
            elementsByNode[e.node]?.let { element -> hovered.add(element); dispatch(element, "mouseenter") }
        }
        interaction.exited.listen { e ->
            elementsByNode[e.node]?.let { element -> hovered.remove(element); dispatch(element, "mouseleave") }
        }
        interaction.pressed.listen { e ->
            for (element in elementPath(e.node)) { active.add(element); dispatch(element, "mousedown") }
        }
        interaction.released.listen { e ->
            active.clear()
            for (element in elementPath(e.node)) dispatch(element, "mouseup")
        }
        interaction.clicked.listen { e -> for (element in elementPath(e.node)) dispatch(element, "click") }
    }

    /** sets the hover state of [element], for programs that do not use [listenTo] */
    fun setHovered(element: Element, hovered: Boolean) { if (hovered) this.hovered.add(element) else this.hovered.remove(element) }

    /** the element that [node] is converted from */
    fun element(node: CompositionNode): Element? = elementsByNode[node]

    /**
     * Sets the values of the composition at [seconds], the time of the document: animations that are active and
     * frozen, CSS animations, rules for pointer states and their transitions
     */
    fun update(seconds: Double) {
        time = seconds
        for (target in targets.values) target.restore()

        // rules for pointer states, and transitions to their values
        for (element in stateRules.map { it.element }.distinct()) {
            val target = target(element) ?: continue
            val declarations = stateDeclarations(element)
            val properties = stateRules.filter { it.element === element }.flatMap { r -> r.declarations.map { it.first } }.distinct()
            val last = targetValues.getOrPut(element) { mutableMapOf() }
            val shown = displayed.getOrPut(element) { mutableMapOf() }
            val running = transitions.getOrPut(element) { mutableMapOf() }
            for (property in properties) {
                val value = declarations.lastOrNull { it.first == property }?.second ?: target.baseValue(property)
                val previous = last[property]
                if (previous != null && previous != value) {
                    // the transition of the new state, from the value that is shown
                    val settings = transitionSettings(declarations, property)
                    if (settings != null) running[property] = RunningTransition(shown[property] ?: previous, value, seconds, settings)
                    else running.remove(property)
                }
                last[property] = value
                val transition = running[property]
                val current = if (transition == null) value else {
                    val t = ((seconds - transition.start - transition.settings.delay) / transition.settings.duration)
                    when {
                        t >= 1.0 -> { running.remove(property); value }
                        t <= 0.0 -> transition.from
                        else -> ValueInterpolation.interpolate(transition.from, transition.to, transition.settings.easing.ease(t))
                    }
                }
                shown[property] = current
                target.set(property, current)
            }
        }

        // CSS animations
        for (animation in css) {
            val values = animation.values(seconds) ?: continue
            for ((property, value) in values) animation.target.set(property, value)
        }

        // SMIL animations: syncbases are resolved in order, every animation applies its value
        resolveSyncbases()
        for (a in smil) applySmil(a, seconds)
    }

    private fun resolveSyncbases() {
        val byId = smil.filter { it.id.isNotEmpty() }.associateBy { it.id }
        // a few rounds, syncbases can depend on each other
        repeat(3) {
            for (a in smil) {
                for ((conditions, instances) in listOf(a.begins to a.beginInstances, a.ends to a.endInstances)) {
                    for (c in conditions) {
                        if (c !is TimeCondition.Sync) continue
                        val base = byId[c.id] ?: continue
                        val begin = base.beginInstances.filter { it <= time }.maxOrNull() ?: base.beginInstances.minOrNull() ?: continue
                        val t = (if (c.end) begin + base.activeDuration(begin) else begin) + c.offset
                        if (t.isFinite() && instances.none { abs(it - t) < 1E-9 }) instances.add(t)
                    }
                }
            }
        }
    }

    private fun applySmil(a: SmilAnimation, seconds: Double) {
        // the interval that begins last, at or before the time
        val begin = a.beginInstances.filter { it <= seconds }.maxOrNull() ?: return
        val active = a.activeDuration(begin)
        val simple = a.duration
        val elapsed = seconds - begin
        val fraction: Double
        val iteration: Int
        if (elapsed < active) {
            if (simple == null || simple.isInfinite()) {
                fraction = 0.0; iteration = 0
            } else {
                iteration = floor(elapsed / simple).toInt()
                fraction = elapsed / simple - iteration
            }
        } else {
            if (!a.freeze) return
            // frozen at the end of the active duration
            if (simple == null || simple.isInfinite() || active.isInfinite()) {
                fraction = if (simple == null) 0.0 else 1.0; iteration = 0
            } else {
                val iterations = active / simple
                iteration = if (iterations == floor(iterations)) iterations.toInt() - 1 else floor(iterations).toInt()
                fraction = if (iterations == floor(iterations)) 1.0 else iterations - floor(iterations)
            }
        }
        if (a.attribute == "motion") {
            applyMotion(a, fraction)
            return
        }
        val value = a.value(fraction, iteration) ?: return
        val result = when {
            !a.additive || a.toAnimation -> value
            a.transformType != null -> listOf(a.target.baseValue("transform"), value).filter { it.isNotBlank() }.joinToString(" ")
            else -> ValueInterpolation.add(a.target.baseValue(a.attribute), value) ?: value
        }
        // an animated transform replaces the transform, unless it adds to it
        a.target.set(a.attribute, result)
    }

    private fun applyMotion(a: SmilAnimation, fraction: Double) {
        val path = a.motionPath ?: return
        // the distance along the path: by key points, or paced
        val keyPoints = a.keyPoints
        val distance = if (keyPoints != null && keyPoints.size >= 2) {
            val times = a.keyTimes?.takeIf { it.size == keyPoints.size } ?: keyPoints.indices.map { it.toDouble() / (keyPoints.size - 1) }
            val i = times.indexOfLast { it <= fraction }.coerceIn(0, keyPoints.size - 2)
            val span = times[i + 1] - times[i]
            val local = if (span <= 0.0) 1.0 else ((fraction - times[i]) / span).coerceIn(0.0, 1.0)
            keyPoints[i] + (keyPoints[i + 1] - keyPoints[i]) * local
        } else fraction
        val t = path.tForLength(distance.coerceIn(0.0, 1.0) * path.length)
        val point = path.position(t)
        // the direction of the path, from the positions around the point
        val e = 1E-4
        val tangent = path.position((t + e).coerceAtMost(1.0)) - path.position((t - e).coerceAtLeast(0.0))
        val angle = when (val rotate = a.motionRotate) {
            null -> 0.0
            "auto" -> Math.toDegrees(atan2(tangent.y, tangent.x))
            "auto-reverse" -> Math.toDegrees(atan2(tangent.y, tangent.x)) + 180.0
            else -> rotate.toDoubleOrNull() ?: 0.0
        }
        a.target.set("motion", "translate(${point.x} ${point.y}) rotate($angle)")
    }
}

/** the animation of a composition loaded from an SVG document, null when it has no animations */
val Composition.animation: SVGAnimation?
    get() = (root as? GroupNode)?.userData?.get("svg-animation") as? SVGAnimation

/**
 * Sets the values of the animations of this composition at [seconds], the time of the document, see [SVGAnimation]
 */
fun Composition.evaluate(seconds: Double) {
    animation?.update(seconds)
}
