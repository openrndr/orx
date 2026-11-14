package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.ShadeStyleFilter1to1
import org.openrndr.draw.createEquivalent
import org.openrndr.math.Vector2

/**
 * A filter with preset slots: presets that vary what it does per pixel, like the radius of a blur. A graph or an
 * [EffectChain] treats it as a pass of its own, or several, sets its slots, and sets the values of [fxContextLibrary]
 * before applying it.
 */
interface PresetFilter {
    /** the preset slots of the filter, in the order they are shown */
    val presetSlots: List<PresetSlot<*>>

    /** sets the values of [fxContextLibrary] for the next apply, see [setFxContext] */
    fun setFxContext(time: Double, frame: Int, resolution: Vector2)
}

/**
 * A filter of several passes, each a shade style, with preset slots that every pass reads: like a separable blur, of
 * which both passes read the radius. Make the slots with [slot], apply the passes with [applyPass], and keep the images
 * between passes in [intermediate]s. The fragment transforms of the passes end with [FX_CONTEXT_USE], so that the
 * presets in the slots can read [fxContextLibrary].
 */
abstract class MultiPassPresetFilter(passStyles: List<ShadeStyle>) : Filter1to1(null), PresetFilter {
    /** the passes, a filter for every shade style */
    protected val passes: List<ShadeStyleFilter1to1> = passStyles.map { style ->
        style.fragmentTransform = (style.fragmentTransform ?: "") + "\n" + FX_CONTEXT_USE
        ShadeStyleFilter1to1(style)
    }

    private val slots = mutableListOf<PresetSlot<*>>()
    override val presetSlots: List<PresetSlot<*>> get() = slots

    init {
        setFxContext(0.0, 0, Vector2(1.0, 1.0))
    }

    /**
     * a slot for presets with [contract] that all passes read as the parameter [name], for a property:
     * `var radiusScale: ScalarPreset by slot("radiusScale", ScalarPreset.CONTRACT, ConstantScalar(1.0))`
     */
    protected fun <P : Preset> slot(name: String, contract: GlslContract, initial: P): PresetSlot<P> =
        PresetSlot(passes, name, contract, initial).also { slots.add(it) }

    override fun setFxContext(time: Double, frame: Int, resolution: Vector2) {
        for (pass in passes) pass.setFxContext(time, frame, resolution)
    }

    /** applies the pass [index] to [source], into [target] */
    protected fun applyPass(index: Int, source: ColorBuffer, target: ColorBuffer, clip: org.openrndr.shape.Rectangle? = null) {
        passes[index].apply(arrayOf(source), arrayOf(target), clip)
    }

    private val intermediates = mutableMapOf<Int, ColorBuffer>()

    /** the image [index] between passes, like [target] in size and format, made again when [target] changes */
    protected fun intermediate(index: Int, target: ColorBuffer): ColorBuffer {
        val current = intermediates[index]
        if (current != null && current.width == target.width && current.height == target.height &&
            current.contentScale == target.contentScale && current.format == target.format && current.type == target.type
        ) return current
        current?.destroy()
        return target.createEquivalent().also { intermediates[index] = it }
    }

    override fun destroy() {
        intermediates.values.forEach { it.destroy() }
        intermediates.clear()
        super.destroy()
    }
}
