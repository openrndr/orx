import org.openrndr.draw.Filter1to1
import org.openrndr.extra.parameters.listParameters
import org.openrndr.extra.shadestyles.presets.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPresetBatch2 {
    @Test
    fun presetsHaveTheirContracts() {
        for (p in listOf(Sepia(), Duotone(), LumaThreshold(), Dither(), FilmGrain(), LumaHalftone(), Masked())) {
            assertTrue(ColorPreset.CONTRACT.accepts(p.dynamicParameter), p::class.simpleName)
        }
        for (p in listOf(LinearGradientMask(), RadialGradientMask(), NoiseMask(), InvertMask())) {
            assertTrue(MaskPreset.CONTRACT.accepts(p.dynamicParameter), p::class.simpleName)
        }
        assertTrue(SamplerPreset.CONTRACT.accepts(ChromaticAberration().dynamicParameter))
    }

    @Test
    fun maskedNestsAColorAndAMask() {
        assertEquals(listOf("color", "mask"), Masked().listParameters().map { it.label })
        assertEquals(listOf("mask"), InvertMask().listParameters().map { it.label })
    }

    @Test
    fun sampleStagesStartPasses() {
        val blur = object : Filter1to1(null) {}
        assertEquals(1, effectChain { sample(ChromaticAberration()); color(Grade()) }.passCount)
        assertEquals(2, effectChain { color(Grade()); sample(ChromaticAberration()); color(Sepia()) }.passCount)
        assertEquals(3, effectChain { sample(ChromaticAberration()); filter(blur); sample(ChromaticAberration()) }.passCount)
    }
}
