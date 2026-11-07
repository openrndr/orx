package org.openrndr.panel.style

import org.openrndr.extra.style.CssKeyframe
import org.openrndr.extra.style.TimingFunction
import org.openrndr.extra.style.splitCssList
import org.openrndr.extra.style.splitCssWords

/** the direction of the iterations of an animation */
enum class AnimationDirection { NORMAL, REVERSE, ALTERNATE, ALTERNATE_REVERSE }

/** whether the values of an animation apply before it starts, after it ends, or both */
enum class AnimationFillMode { NONE, FORWARDS, BACKWARDS, BOTH }

/**
 * The @keyframes rules of the style sheets of panels, by name; [styleSheets] adds the rules of the CSS it reads. A
 * later rule with the same name replaces an earlier one.
 */
object PanelKeyframes {
    private val rules = HashMap<String, List<CssKeyframe>>()

    /** the keyframes of the rule [name], null when there is none */
    operator fun get(name: String): List<CssKeyframe>? = rules[name]

    /** adds the keyframes [frames] of the rule [name] */
    operator fun set(name: String, frames: List<CssKeyframe>) {
        rules[name] = frames
    }
}

/** whether an animation runs or is paused, a paused animation keeps its time */
enum class AnimationPlayState { RUNNING, PAUSED }

/** a list of CSS values of [parse], separated by commas */
private fun <T> list(parse: (String) -> T?): (String) -> List<T>? = { text -> splitCssList(text).map { parse(it.trim()) ?: return@map null }.let { if (null in it) null else it.filterNotNull() } }

/**
 * The names of the animations of the element, `none` for none. The other animation properties are lists too, their
 * values repeat for the animations that they do not give, as in CSS.
 */
var StyleSheet.animationName by PropertyHandler("animation-name", PropertyInheritance.RESET, listOf("none"),
    list { it.takeIf { v -> v.isNotEmpty() && ' ' !in v } })

/** the time in seconds an iteration of an animation takes */
var StyleSheet.animationDuration by PropertyHandler("animation-duration", PropertyInheritance.RESET, listOf(0.0), list(::parseTime))

/** the time in seconds before an animation starts */
var StyleSheet.animationDelay by PropertyHandler("animation-delay", PropertyInheritance.RESET, listOf(0.0), list(::parseTime))

/** how an animation moves from keyframe to keyframe */
var StyleSheet.animationTimingFunction by PropertyHandler("animation-timing-function", PropertyInheritance.RESET, listOf(TimingFunction.EASE), list(::parseTimingFunction))

/** the number of iterations of an animation, infinite for [Double.POSITIVE_INFINITY] */
var StyleSheet.animationIterationCount by PropertyHandler("animation-iteration-count", PropertyInheritance.RESET, listOf(1.0),
    list { if (it.lowercase() == "infinite") Double.POSITIVE_INFINITY else parseNumber(it)?.takeIf { n -> n >= 0.0 } })

var StyleSheet.animationDirection by PropertyHandler("animation-direction", PropertyInheritance.RESET, listOf(AnimationDirection.NORMAL),
    list(keywords("normal" to AnimationDirection.NORMAL, "reverse" to AnimationDirection.REVERSE,
        "alternate" to AnimationDirection.ALTERNATE, "alternate-reverse" to AnimationDirection.ALTERNATE_REVERSE)))

var StyleSheet.animationFillMode by PropertyHandler("animation-fill-mode", PropertyInheritance.RESET, listOf(AnimationFillMode.NONE),
    list(keywords("none" to AnimationFillMode.NONE, "forwards" to AnimationFillMode.FORWARDS,
        "backwards" to AnimationFillMode.BACKWARDS, "both" to AnimationFillMode.BOTH)))

var StyleSheet.animationPlayState by PropertyHandler("animation-play-state", PropertyInheritance.RESET, listOf(AnimationPlayState.RUNNING),
    list(keywords("running" to AnimationPlayState.RUNNING, "paused" to AnimationPlayState.PAUSED)))

private val directions = setOf("normal", "reverse", "alternate", "alternate-reverse")
private val fillModes = setOf("none", "forwards", "backwards", "both")
private val playStates = setOf("running", "paused")

// the animation shorthand: every animation of the list sets the values of the longhands at its place
private val animationShorthand = PanelProperties.apply {
    val longhands = listOf("animation-name", "animation-duration", "animation-delay", "animation-timing-function",
        "animation-iteration-count", "animation-direction", "animation-fill-mode", "animation-play-state")
    shorthand("animation", longhands) { value ->
        val values = longhands.associateWith { mutableListOf<String>() }
        for (part in splitCssList(value)) {
            val given = mutableMapOf<String, String>()
            var times = 0
            for (word in splitCssWords(part.trim())) {
                val w = word.lowercase()
                when {
                    parseTime(w) != null -> { given[if (times == 0) "animation-duration" else "animation-delay"] = w; times++ }
                    parseTimingFunction(w) != null -> given["animation-timing-function"] = w
                    w == "infinite" || parseNumber(w) != null -> given["animation-iteration-count"] = w
                    w in directions -> given["animation-direction"] = w
                    w in fillModes && w != "none" -> given["animation-fill-mode"] = w
                    w in playStates -> given["animation-play-state"] = w
                    else -> given["animation-name"] = word
                }
            }
            val defaults = mapOf("animation-name" to "none", "animation-duration" to "0s", "animation-delay" to "0s",
                "animation-timing-function" to "ease", "animation-iteration-count" to "1", "animation-direction" to "normal",
                "animation-fill-mode" to "none", "animation-play-state" to "running")
            for (longhand in longhands) values[longhand]!!.add(given[longhand] ?: defaults[longhand]!!)
        }
        longhands.map { it to values[it]!!.joinToString(", ") }
    }
}

/** initializes the delegates of this file, which register the animation properties */
internal fun ensureAnimationProperties() = animationShorthand
