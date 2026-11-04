@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.OptionParameter
import org.openrndr.math.Vector4

/**
 * A blend: a function `vec4 blend(vec4 base, vec4 layer)` that combines a layer color with a base color
 */
abstract class BlendPreset : Preset {
    abstract override val dynamicParameter: GlslFunction
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("vec4", "vec4", "vec4")
    }
}

/** the blend modes of [LayerBlend] */
enum class LayerBlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    SOFT_LIGHT,
    ADD,
    DIFFERENCE
}

/**
 * Blends the rgb channels of the layer with those of the base by [mode], weighted by the alpha of the layer and
 * [opacity]. The alpha of the base is kept. Changing the mode does not compile another shader.
 */
class LayerBlend(mode: LayerBlendMode = LayerBlendMode.NORMAL) : BlendPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 base, vec4 layer", """
        vec3 a = base.rgb;
        vec3 b = layer.rgb;
        vec3 r = b;
        if (p_mode == 1) {
            r = a * b;
        } else if (p_mode == 2) {
            r = 1.0 - (1.0 - a) * (1.0 - b);
        } else if (p_mode == 3) {
            r = mix(2.0 * a * b, 1.0 - 2.0 * (1.0 - a) * (1.0 - b), step(0.5, a));
        } else if (p_mode == 4) {
            r = (1.0 - 2.0 * b) * a * a + 2.0 * b * a;
        } else if (p_mode == 5) {
            r = min(a + b, 1.0);
        } else if (p_mode == 6) {
            r = abs(a - b);
        }
        return vec4(mix(a, r, layer.a * p_opacity), base.a);
        """.trimIndent()
    )

    @OptionParameter("mode", order = 0)
    var mode: LayerBlendMode = mode
        set(value) {
            field = value
            dynamicParameter.parameter("mode", value.ordinal)
        }

    @DoubleParameter("opacity", 0.0, 1.0, order = 1)
    var opacity: Double by dynamicParameter.Parameter("opacity", 1.0)

    init {
        dynamicParameter.parameter("mode", mode.ordinal)
    }
}
