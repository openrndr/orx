@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.NestedParameter
import org.openrndr.extra.shadestyles.presets.ConstantScalar
import org.openrndr.extra.shadestyles.presets.MultiPassPresetFilter
import org.openrndr.extra.shadestyles.presets.ScalarPreset
import org.openrndr.extra.shadestyles.presets.straightColorLibrary
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/** a pass of [ApproximateGaussianBlur], along [direction] */
private fun gaussianPass(direction: Vector2) = shadeStyle {
    libraries = listOf(straightColorLibrary)
    fragmentTransform = """
        vec2 s = 1.0 / vec2(textureSize(p_input0, 0));
        float sg = p_sigma * p_sigmaScale;
        // the input holds premultiplied colors, which are averaged, the result is made straight, the filter
        // premultiplies it when it writes it
        if (sg <= 1e-4) {
            x_fill = straight(texture(p_input0, c_textureCoordinate) * p_gain);
        } else {
            vec4 sum = vec4(0.0);
            float weight = 0.0;
            for (int x = -p_window; x <= p_window; ++x) {
                float lw = exp(float(-(x * x)) / (2.0 * sg * sg));
                vec2 tc = c_textureCoordinate + float(x) * p_blurDirection * s;
                if (p_wrapU != 0) {
                    tc.x = mod(tc.x, 1.0);
                }
                if (p_wrapV != 0) {
                    tc.y = mod(tc.y, 1.0);
                }
                sum += texture(p_input0, tc) * lw;
                weight += lw;
            }
            x_fill = straight((sum / weight) * p_gain);
        }
    """.trimIndent()
    parameter("blurDirection", direction)
}

/**
 * Approximate separated Gaussian blur, in a horizontal and a vertical pass. The sigma can vary per pixel by
 * [sigmaScale], a preset that both passes read, for example from a texture:
 *
 * ```
 * blur.sigmaScale = TextureScalar(depthOfField)   // sharp where the red channel is 0
 * ```
 *
 * A sigma of 0 leaves the image as it is.
 */
@Description("Approximate Gaussian blur")
class ApproximateGaussianBlur : MultiPassPresetFilter(listOf(gaussianPass(Vector2(1.0, 0.0)), gaussianPass(Vector2(0.0, 1.0)))) {

    @BooleanParameter("wrap u")
    var wrapU = false

    @BooleanParameter("wrap v")
    var wrapV = false

    /** sets [name] to [value] in both passes */
    private fun set(name: String, value: Int) {
        for (pass in passes) pass.parameter(name, value)
    }

    private fun set(name: String, value: Double) {
        for (pass in passes) pass.parameter(name, value)
    }

    /**
     * blur sample window, default value is 5
     */
    @IntParameter("window size", 1, 25)
    var window: Int = 5
        set(value) {
            field = value
            set("window", value)
        }

    /**
     * spread multiplier, default value is 1.0; the samples are a pixel apart whatever it is, as they were
     */
    @DoubleParameter("kernel spread", 1.0, 4.0)
    var spread: Double = 1.0

    /**
     * blur sigma, default value is 1.0
     */
    @DoubleParameter("kernel sigma", 0.0, 25.0)
    var sigma: Double = 1.0
        set(value) {
            field = value
            set("sigma", value)
        }

    /**
     * post blur gain, default value is 1.0
     */
    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double = 1.0
        set(value) {
            field = value
            set("gain", value)
        }

    /**
     * Scale of the sigma, can vary per pixel, for example with `TextureScalar(map)`. The default is a [ConstantScalar]
     * of 1.0.
     */
    @NestedParameter("sigma scale")
    var sigmaScale: ScalarPreset by slot("sigmaScale", ScalarPreset.CONTRACT, ConstantScalar(1.0))

    init {
        set("window", window)
        set("sigma", sigma)
        set("gain", gain)
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        set("wrapU", if (wrapU) 1 else 0)
        set("wrapV", if (wrapV) 1 else 0)
        val between = intermediate(0, target[0])
        applyPass(0, source[0], between, clip)
        applyPass(1, between, target[0], clip)
    }
}
