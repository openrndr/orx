import org.openrndr.draw.glslFunction
import org.openrndr.draw.glslParameter
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.parameters.ParameterType
import org.openrndr.extra.parameters.listParameters
import org.openrndr.extra.shadestyles.presets.*
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestPresets {
    @Test
    fun contractsAcceptMatchingSignatures() {
        val warp = GlslContract("vec2", "vec2")
        assertTrue(warp.accepts(glslFunction<Vector2>("vec2 p", "p")))
        assertFalse(warp.accepts(glslFunction<Double>("vec2 p", "p.x")))
        assertFalse(warp.accepts(glslFunction<Vector2>("vec2 p, float t", "p")))
        assertFalse(warp.accepts(glslParameter<Vector2>("vec2(0.0)")))

        val scalar = GlslContract("float")
        assertTrue(scalar.accepts(glslParameter<Double>("1.0")))
        assertFalse(scalar.accepts(glslParameter<Vector2>("vec2(0.0)")))
        assertFalse(scalar.accepts(glslFunction<Double>("float x", "x")))
    }

    @Test
    fun slotsSetPresetsAsParameters() {
        val style = shadeStyle { }
        val ripple = Ripple()
        val holder = object {
            var warp: WarpPreset by style.presetSlot("warp", WarpPreset.CONTRACT, NoiseWarp())
        }
        holder.warp = ripple
        assertSame(ripple, holder.warp)
        assertSame(ripple, style.parameterValues["warp"])
    }

    @Test
    fun slotsRejectPresetsWithAnotherSignature() {
        val style = shadeStyle { }
        val holder = object {
            var scale: ScalarPreset by style.presetSlot("scale", ScalarPreset.CONTRACT, ConstantScalar(1.0))
        }
        val wrong = object : ScalarPreset() {
            override val dynamicParameter = glslParameter<Vector2>("vec2(0.0)")
        }
        val e = assertFailsWith<IllegalArgumentException> { holder.scale = wrong }
        assertEquals("slot 'scale' takes a float, the preset provides a vec2", e.message)
        assertTrue(holder.scale is ConstantScalar)
    }

    @Test
    fun presetsListTheirParameters() {
        val noise = NoiseWarp().listParameters()
        assertEquals(
            listOf("seed", "scale", "phase", "radius", "lacunarity", "gain", "decay", "octaves"),
            noise.map { it.property?.name })

        val chain = ChainWarp(Ripple(), Swirl()).listParameters()
        assertEquals(listOf(ParameterType.Nested, ParameterType.Nested), chain.map { it.parameterType })
        assertEquals(listOf("first", "second"), chain.map { it.label })
        assertEquals("amount", chain[0].nestedParameters!!.first().property?.name)
    }

    @Test
    fun annotatedPropertiesSetFunctionParameters() {
        val ripple = Ripple()
        ripple.amount = 0.05
        assertEquals(0.05, ripple.dynamicParameter.parameterValues["amount"])
        val constant = ConstantScalar(0.25)
        assertEquals(0.25, constant.dynamicParameter.parameterValues["value"])
    }
}
