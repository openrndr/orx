import org.openrndr.draw.Filter1to1
import org.openrndr.extra.parameters.ParameterType
import org.openrndr.extra.parameters.listParameters
import org.openrndr.extra.shadestyles.presets.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPresetBatch1 {
    @Test
    fun presetsHaveTheirContracts() {
        for (p in listOf(Affine(), Kaleidoscope(), Pixelate(), Wave(), Fisheye())) {
            assertTrue(WarpPreset.CONTRACT.accepts(p.dynamicParameter), p::class.simpleName)
        }
        for (p in listOf(Colormap(), OklchAdjust(), Levels(), Layer())) {
            assertTrue(ColorPreset.CONTRACT.accepts(p.dynamicParameter), p::class.simpleName)
        }
        for (p in listOf(LinearGradientScalar(), RadialGradientScalar(), NoiseScalar())) {
            assertTrue(ScalarPreset.CONTRACT.accepts(p.dynamicParameter), p::class.simpleName)
        }
        for (p in listOf(SolidSource(), LinearGradientSource(), RadialGradientSource(), CheckersSource(), NoiseSource())) {
            assertTrue(SourcePreset.CONTRACT.accepts(p.dynamicParameter), p::class.simpleName)
        }
        assertTrue(BlendPreset.CONTRACT.accepts(LayerBlend().dynamicParameter))
    }

    @Test
    fun optionsSetIntegerParameters() {
        val colormap = Colormap()
        assertEquals(0, colormap.dynamicParameter.parameterValues["colormap"])
        colormap.colormap = ColormapType.GRAYSCALE
        assertEquals(2, colormap.dynamicParameter.parameterValues["colormap"])

        val blend = LayerBlend(LayerBlendMode.SCREEN)
        assertEquals(2, blend.dynamicParameter.parameterValues["mode"])
        assertEquals(ParameterType.Option, blend.listParameters().first().parameterType)
    }

    @Test
    fun layersNestASourceAndABlend() {
        assertEquals(listOf("source", "blend"), Layer().listParameters().map { it.label })
    }

    @Test
    fun sourceStagesFuse() {
        val blur = object : Filter1to1(null) {}
        assertEquals(1, effectChain { warp(Pixelate()); source(CheckersSource()); color(Grade()) }.passCount)
        assertEquals(2, effectChain { source(NoiseSource()); filter(blur) }.passCount)
    }
}
