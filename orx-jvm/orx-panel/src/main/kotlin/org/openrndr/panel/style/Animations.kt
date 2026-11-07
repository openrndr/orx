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

/** the name of the animation, `none` for no animation */
var StyleSheet.animationName by PropertyHandler("animation-name", PropertyInheritance.RESET, "none",
    { it.trim().takeIf { v -> v.isNotEmpty() && ' ' !in v && ',' !in v } })

/** the time in seconds an iteration of the animation takes */
var StyleSheet.animationDuration by PropertyHandler("animation-duration", PropertyInheritance.RESET, 0.0, ::parseTime)

/** the time in seconds before the animation starts */
var StyleSheet.animationDelay by PropertyHandler("animation-delay", PropertyInheritance.RESET, 0.0, ::parseTime)

/** how the animation moves from keyframe to keyframe */
var StyleSheet.animationTimingFunction by PropertyHandler("animation-timing-function", PropertyInheritance.RESET, TimingFunction.EASE, ::parseTimingFunction)

/** the number of iterations of the animation, infinite for [Double.POSITIVE_INFINITY] */
var StyleSheet.animationIterationCount by PropertyHandler("animation-iteration-count", PropertyInheritance.RESET, 1.0,
    { if (it.trim().lowercase() == "infinite") Double.POSITIVE_INFINITY else parseNumber(it)?.takeIf { n -> n >= 0.0 } })

var StyleSheet.animationDirection by PropertyHandler("animation-direction", PropertyInheritance.RESET, AnimationDirection.NORMAL,
    keywords("normal" to AnimationDirection.NORMAL, "reverse" to AnimationDirection.REVERSE,
        "alternate" to AnimationDirection.ALTERNATE, "alternate-reverse" to AnimationDirection.ALTERNATE_REVERSE))

var StyleSheet.animationFillMode by PropertyHandler("animation-fill-mode", PropertyInheritance.RESET, AnimationFillMode.NONE,
    keywords("none" to AnimationFillMode.NONE, "forwards" to AnimationFillMode.FORWARDS,
        "backwards" to AnimationFillMode.BACKWARDS, "both" to AnimationFillMode.BOTH))

private val directions = setOf("normal", "reverse", "alternate", "alternate-reverse")
private val fillModes = setOf("none", "forwards", "backwards", "both")

// the animation shorthand: the first animation of a list, panels run one animation per element
private val animationShorthand = PanelProperties.apply {
    shorthand("animation", listOf("animation-name", "animation-duration", "animation-delay", "animation-timing-function",
        "animation-iteration-count", "animation-direction", "animation-fill-mode")) { value ->
        val words = splitCssWords(splitCssList(value).first().trim())
        val result = mutableListOf<Pair<String, String>>()
        var times = 0
        for (word in words) {
            val w = word.lowercase()
            when {
                parseTime(w) != null -> { result.add((if (times == 0) "animation-duration" else "animation-delay") to w); times++ }
                parseTimingFunction(w) != null -> result.add("animation-timing-function" to w)
                w == "infinite" || parseNumber(w) != null -> result.add("animation-iteration-count" to w)
                w in directions -> result.add("animation-direction" to w)
                w in fillModes && w != "none" -> result.add("animation-fill-mode" to w)
                else -> result.add("animation-name" to word)
            }
        }
        result
    }
}

/** initializes the delegates of this file, which register the animation properties */
internal fun ensureAnimationProperties() = animationShorthand
