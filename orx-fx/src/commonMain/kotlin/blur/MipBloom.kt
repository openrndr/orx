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
    var bias: ColorRGBa by parameters

    init {
        bias = ColorRGBa.BLACK
        gain = 1.0
    }
}

@Description("MipBloom")
open class MipBloom<T : Filter>(val blur: T) : Filter(mppFilterShader(fx_bloom_combine, "bloom-combine")) {
    var passes = 6

    @DoubleParameter("shape", 0.0, 4.0)
    var shape: Double = 1.0

    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double = 1.0

    /**
     * noise gain. low noise gains will result in visible banding of the image. default value is 0.25
     */
    @DoubleParameter("noise gain", 0.0, 1.0)
    var noiseGain: Double = 0.25

    @DoubleParameter("noise seed", 0.0, 1000.0)
    var noiseSeed: Double = 0.0

    @BooleanParameter("sRGB")
    var sRGB = true

    var intermediates = mutableListOf<ColorBuffer>()
    var sourceCopy: ColorBuffer? = null

    val upscale = BloomUpscale()
    val downScale = BloomDownscale()
    val combine = BloomCombine()

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        sourceCopy?.let {
            if (!it.isEquivalentTo(source[0], ignoreType = true)) {
                it.destroy()
                sourceCopy = null
            }
        }
        if (sourceCopy == null) {
            sourceCopy = source[0].createEquivalent(type = ColorType.FLOAT16)
        }

        source[0].copyTo(sourceCopy!!)

        upscale.shape = shape
        if (intermediates.size != passes
                || (intermediates.isNotEmpty() && (!intermediates[0].isEquivalentTo(target[0], ignoreType = true, ignoreFormat = true)))) {
            intermediates.forEach {
                it.destroy()
            }
            intermediates.clear()

            for (pass in 0 until passes) {
                val tdiv = 1 shl (pass + 1)
                val cb = colorBuffer(target[0].width / tdiv, target[0].height / tdiv, type = ColorType.FLOAT16)
                intermediates.add(cb)
            }
        }


        if (sRGB) {
            linearize.apply(sourceCopy!!, sourceCopy!!)
        }

        upscale.noiseGain = noiseGain
        upscale.noiseSeed = noiseSeed
        downScale.apply(sourceCopy!!, intermediates[0])
        blur.apply(intermediates[0], intermediates[0])

        for (pass in 1 until passes) {
            downScale.apply(intermediates[pass - 1], intermediates[pass])
            blur.apply(intermediates[pass], intermediates[pass])
        }

        upscale.apply(intermediates.toTypedArray(), arrayOf(target[0]))
        combine.gain = gain
        combine.apply(arrayOf(sourceCopy!!, target[0]), target)

        if (sRGB) {
            delinearize.apply(target[0], target[0])
        }
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

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        blur.radius = radius
        blur.samples = samples
        super.apply(source, target)
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

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        blur.window = window
        blur.sigma = sigma
        super.apply(source, target)
    }
}