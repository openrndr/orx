package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.DynamicParameter
import org.openrndr.draw.DynamicParameterSource
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.StyleParameters
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The GLSL signature a preset slot accepts: a function returning [returnType] that takes [argumentTypes], or, without
 * argument types, a value of [returnType] such as a GLSL parameter or a texture parameter.
 */
class GlslContract(val returnType: String, vararg val argumentTypes: String) {
    /** true when [parameter] has this signature */
    fun accepts(parameter: DynamicParameter): Boolean = when (parameter) {
        is GlslFunction -> parameter.hasContract(returnType, *argumentTypes)
        else -> argumentTypes.isEmpty() && parameter.glslType == returnType
    }

    override fun toString(): String =
        if (argumentTypes.isEmpty()) returnType else "$returnType(${argumentTypes.joinToString(", ")})"
}

/** the signature of [parameter] in the notation of [GlslContract] */
private fun signatureOf(parameter: DynamicParameter): String =
    if (parameter is GlslFunction) "${parameter.glslType}(${parameter.argumentTypes.joinToString(", ")})" else parameter.glslType

/**
 * A preset: an object with annotated properties that provides a [DynamicParameter] for a slot with [contract].
 * Presets are assigned to the slots of filters, shade styles and other presets, see [presetSlot].
 */
interface Preset : DynamicParameterSource {
    /** the signature of the dynamic parameter of this preset */
    val contract: GlslContract
}

/**
 * A property that holds a preset and sets it as the parameter [name] of [parameters], a shade style, a filter or the
 * function of another preset. Assigned presets must have [contract].
 */
class PresetSlot<P : Preset>(
    private val parameters: StyleParameters,
    val name: String,
    val contract: GlslContract,
    initial: P
) : ReadWriteProperty<Any?, P> {
    private var value: P = initial

    init {
        set(initial)
    }

    private fun set(preset: P) {
        val parameter = preset.dynamicParameter
        require(contract.accepts(parameter)) {
            "slot '$name' takes a $contract, ${preset::class.simpleName ?: "the preset"} provides a ${signatureOf(parameter)}"
        }
        value = preset
        parameters.parameter(name, preset)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): P = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: P) = set(value)
}

/**
 * A slot for presets with [contract], set as parameter [name]. Annotate the property with `@NestedParameter` to show
 * the parameters of the preset in user interfaces:
 *
 * ```
 * @NestedParameter("warp")
 * var warp: WarpPreset by presetSlot("warp", WarpPreset.CONTRACT, NoiseWarp())
 * // in the shade style: vec2 uv = p_warp(c_textureCoordinate);
 * ```
 */
fun <P : Preset> StyleParameters.presetSlot(name: String, contract: GlslContract, initial: P): PresetSlot<P> =
    PresetSlot(this, name, contract, initial)
