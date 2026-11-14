package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.DynamicParameter
import org.openrndr.draw.DynamicParameterSource
import org.openrndr.draw.GlslLibrary
import org.openrndr.draw.StyleParameters
import org.openrndr.draw.glslLibrary
import org.openrndr.math.Vector2

/**
 * The context of the image an effect makes, for presets that change over time or depend on the size of the image:
 *
 * - `float fx_time()`: the time in seconds
 * - `int fx_frame()`: the number of the frame
 * - `vec2 fx_resolution()`: the size of the image in pixels
 * - `vec2 fx_texel()`: the size of a pixel in texture coordinates
 *
 * A preset that reads them lists this library in the libraries of its GLSL function or parameter:
 *
 * ```
 * override val dynamicParameter = glslFunction<Vector2>("vec2 uv", "return uv + 0.01 * sin(fx_time()) * fx_texel();",
 *     listOf(fxContextLibrary))
 * ```
 *
 * Whoever runs the shader sets the values with [setFxContext], which [EffectChain] and the [PresetFilter]s do on every
 * apply. The shader code then also includes [FX_CONTEXT_USE], so that the values can be set whether or not a preset
 * reads them.
 */
val fxContextLibrary: GlslLibrary = glslLibrary(
    "fx_context", """
    float fx_time() { return p_fxTime; }
    int fx_frame() { return p_fxFrame; }
    vec2 fx_resolution() { return p_fxResolution; }
    vec2 fx_texel() { return 1.0 / p_fxResolution; }
    """.trimIndent()
)

/**
 * A GLSL statement for the end of a fragment transform that reads the values of [fxContextLibrary] without changing the
 * result, so that they are in the shader and can be set when none of its presets read them.
 */
const val FX_CONTEXT_USE = "if (p_fxFrame < -1) { x_fill += vec4(p_fxTime, p_fxResolution, 0.0); }"

/**
 * Sets the values of [fxContextLibrary]: the [time] in seconds, the number of the [frame] and the [resolution] of the
 * image in pixels. For a shade style or a filter whose shader reads them or includes [FX_CONTEXT_USE].
 */
fun StyleParameters.setFxContext(time: Double, frame: Int, resolution: Vector2) {
    parameter("fxTime", time)
    parameter("fxFrame", frame)
    parameter("fxResolution", resolution)
}

/** true when this library is [fxContextLibrary] or depends on it, directly or through other libraries */
fun GlslLibrary.readsFxContext(): Boolean = this === fxContextLibrary || dependencies.any { it.readsFxContext() }

/**
 * true when the code of this parameter can read the values of [fxContextLibrary]: through its libraries, or through
 * those of the dynamic parameters it has as parameters, like the presets in the slots of a preset, at any depth. The
 * presets as they are now: assigning another preset to a slot can change it.
 */
fun DynamicParameter.readsFxContext(): Boolean = readsFxContext(this, mutableSetOf())

private fun readsFxContext(parameter: DynamicParameter, visited: MutableSet<DynamicParameter>): Boolean {
    if (!visited.add(parameter)) return false
    if (parameter.libraries.any { it.readsFxContext() }) return true
    val nested = (parameter as? StyleParameters)?.parameterValues?.values ?: return false
    return nested.any { value ->
        val dynamic = when (value) {
            is DynamicParameter -> value
            is DynamicParameterSource -> value.dynamicParameter
            else -> null
        }
        dynamic != null && readsFxContext(dynamic, visited)
    }
}

/**
 * true when this preset can read the values of [fxContextLibrary], also through the presets in its slots, so that it
 * changes over time even when nothing else does, see [DynamicParameter.readsFxContext]
 */
fun Preset.readsFxContext(): Boolean = dynamicParameter.readsFxContext()

/** true when a preset in one of the slots of this filter can read the values of [fxContextLibrary] */
fun PresetFilter.readsFxContext(): Boolean = presetSlots.any { it.preset.readsFxContext() }
