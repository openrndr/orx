@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_bloom_combine
import org.openrndr.extra.fx.fx_bloom_downscale
import org.openrndr.extra.fx.fx_bloom_upscale
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.filter.color.delinearize
import org.openrndr.filter.color.linearize
import org.openrndr.shape.Rectangle

class BloomDownscale : Filter(mppFilterShader(fx_bloom_downscale,"bloom-downscale"))

class BloomUpscale : Filter(mppFilterShader(fx_bloom_upscale, "bloom-upscale")) {
    var gain: Double by parameters
    var shape: Double by parameters
    var noiseSeed: Double by parameters
    var noiseGain: Double by parameters

    init {
        gain = 1.0
        shape = 1.0
        noiseSeed = 1.0
        noiseGain = 0.25
    }
}

class BloomCombine : Filter(mppFilterShader(fx_bloom_combine, "bloom-combine")) {
    var gain: Double by parameters
    var pregain: Double by parameters
    var bias: ColorRGBa by parameters

    init {
        bias = ColorRGBa.BLACK
        gain = 1.0
        pregain = 1.0
    }
}

@Description("MipBloom")
open class MipBloom<T : Filter>(val blur: T) : Filter1to1(mppFilterShader(fx_bloom_combine, "bloom-combine")) {
    var passes = 6

    @DoubleParameter("shape", 0.0, 4.0)
    var shape: Double = 1.0

    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double = 1.0

    @DoubleParameter("pregain", 0.0, 4.0)
    var pregain: Double = 1.0


    /**
     * noise gain. low noise gains will result in visible banding of the image. default value is 0.25
     */
    @DoubleParameter("noise gain", 0.0, 1.0)
    var noiseGain: Double = 0.25

    @DoubleParameter("noise seed", 0.0, 1000.0)
    var noiseSeed: Double = 0.0
    var intermediates = mutableListOf<ColorBuffer>()
    var blurred = mutableListOf<ColorBuffer>()

    val upscale = BloomUpscale()
    val downScale = BloomDownscale()
    val combine = BloomCombine()

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)

        upscale.shape = shape
        if (intermediates.size != passes
                || (intermediates.isNotEmpty() && (!intermediates[0].isEquivalentTo(target[0], ignoreType = true, ignoreFormat = true)))) {
            intermediates.forEach {
                it.destroy()
            }
            blurred.forEach {
                it.destroy()
            }
            intermediates.clear()
            blurred.clear()

            for (pass in 0 until passes) {
                val tdiv = 1 shl (pass + 1)
                val cb = colorBuffer(target[0].width / tdiv, target[0].height / tdiv, type = ColorType.FLOAT32)
                intermediates.add(cb)
                val cbb = colorBuffer(target[0].width / tdiv, target[0].height / tdiv, type = ColorType.FLOAT32)
                blurred.add(cbb)
            }
        }

        upscale.noiseGain = noiseGain
        upscale.noiseSeed = noiseSeed
        downScale.apply(source[0], intermediates[0], clip)
        blur.apply(intermediates[0], blurred[0], clip)

        for (pass in 1 until passes) {
            downScale.apply(blurred[pass - 1], intermediates[pass], clip)
            blur.apply(intermediates[pass], blurred[pass], clip)
        }

        upscale.apply(blurred.toTypedArray(), arrayOf(target[0]), clip)
        combine.gain = gain
        combine.pregain = pregain
        combine.apply(arrayOf(source[0], target[0]), target, clip)
    }
}

@Description("Hash bloom")
class HashBloom : MipBloom<HashBlur>(blur = HashBlur()) {

    /**
     * Blur radius in pixels, default is 5.0
     */
    @DoubleParameter("blur radius", 1.0, 25.0)
    var radius: Double = 5.0

    /**
     * Number of samples, default is 30
     */
    @IntParameter("number of samples", 1, 100)
    var samples: Int = 30

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        blur.radius = radius
        blur.samples = samples
        super.apply(source, target, clip)
    }
}

@Description("Gaussian bloom")
class GaussianBloom : MipBloom<GaussianBlur>(blur = GaussianBlur()) {
    /**
     * blur sample window, default value is 5
     */
    @IntParameter("window size", 1, 25)
    var window: Int = 5

    /**
     * blur sigma, default value is 1.0
     */
    @DoubleParameter("kernel sigma", 0.0, 25.0)
    var sigma: Double = 1.0

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        blur.window = window
        blur.sigma = sigma
        super.apply(source, target, clip)
    }
}