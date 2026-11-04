import org.openrndr.draw.Filter1to1
import org.openrndr.extra.parameters.listParameters
import org.openrndr.extra.shadestyles.presets.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TestEffectChain {
    private val blur = object : Filter1to1(null) {}

    @Test
    fun fusesConsecutiveWarpAndColorStages() {
        assertEquals(0, EffectChain().passCount)
        assertEquals(1, effectChain { warp(Ripple()); color(Grade()); warp(Swirl()); color(Vignette()) }.passCount)
        assertEquals(1, effectChain { filter(blur) }.passCount)
        assertEquals(3, effectChain { warp(Ripple()); color(Grade()); filter(blur); color(Vignette()) }.passCount)
        assertEquals(3, effectChain { filter(blur); color(Grade()); filter(blur) }.passCount)
    }

    @Test
    fun assigningStagesBuildsPasses() {
        val chain = effectChain { color(Grade()) }
        chain.stages = listOf(EffectStage.Color(Grade()), EffectStage.Filter(blur), EffectStage.Warp(Ripple()))
        assertEquals(3, chain.passCount)
    }

    @Test
    fun colorPresetsListTheirParameters() {
        assertEquals(
            listOf("exposure", "contrast", "saturation", "gamma"),
            Grade().listParameters().map { it.property?.name })
        assertEquals(listOf("first", "second"), ChainColor().listParameters().map { it.label })
    }

    @Test
    fun colorPresetsHaveTheColorContract() {
        for (preset in listOf(Grade(), Tint(), Posterize(), Invert(), Vignette(), ChainColor())) {
            assert(ColorPreset.CONTRACT.accepts(preset.dynamicParameter)) { "${preset::class.simpleName}" }
        }
    }
}
