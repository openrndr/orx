import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.glslFunction
import org.openrndr.draw.glslLibrary
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.shadestyles.presets.*
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestReadsFxContext {
    /** a library that reads the context through its dependency */
    private val pulse = glslLibrary("pulse", "float pulse() { return 0.5 + 0.5 * sin(fx_time()); }", fxContextLibrary)

    /** a mask that reads the context through [pulse] */
    private inner class PulseMask : MaskPreset() {
        override val dynamicParameter = glslFunction<Double>("vec2 uv", "return pulse() * uv.x;", listOf(pulse))
    }

    /** a filter of two passes with a mask slot */
    private class MaskedFilter : MultiPassPresetFilter(listOf(shadeStyle { fragmentTransform = "x_fill = vec4(p_mask(c_textureCoordinate));" })) {
        var mask: MaskPreset by slot("mask", MaskPreset.CONTRACT, RadialGradientMask())
        override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: org.openrndr.shape.Rectangle?) {}
    }

    @Test
    fun `presets read the context through their libraries and their dependencies`() {
        assertTrue(FilmGrain().readsFxContext())
        assertFalse(Grade().readsFxContext())
        assertTrue(fxContextLibrary.readsFxContext())
        assertTrue(pulse.readsFxContext())
        assertFalse(glslLibrary("plain", "float two() { return 2.0; }").readsFxContext())
        assertTrue(PulseMask().readsFxContext())
    }

    @Test
    fun `presets read the context through the presets in their slots, at any depth`() {
        // two levels down
        assertTrue(InvertMask(InvertMask(PulseMask())).readsFxContext())
        assertFalse(InvertMask(InvertMask(RadialGradientMask())).readsFxContext())
        // as the slot changes
        val chain = ChainColor(Grade(), Vignette())
        assertFalse(chain.readsFxContext())
        chain.second = FilmGrain()
        assertTrue(chain.readsFxContext())
        chain.second = Invert()
        assertFalse(chain.readsFxContext())
        // grain or a time-reading mask in a masked color
        assertTrue(Masked(FilmGrain(), RadialGradientMask()).readsFxContext())
        assertTrue(Masked(Grade(), PulseMask()).readsFxContext())
        assertFalse(Masked(Grade(), RadialGradientMask()).readsFxContext())
        // a warp of warps
        assertFalse(ChainWarp(Ripple(), Swirl()).readsFxContext())
    }

    @Test
    fun `filters read the context through the presets in their slots`() {
        val filter = MaskedFilter()
        assertFalse(filter.readsFxContext())
        filter.mask = InvertMask(PulseMask())
        assertTrue(filter.readsFxContext())
    }
}
