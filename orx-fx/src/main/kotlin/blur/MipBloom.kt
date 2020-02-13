package org.openrndr.extra.fx.blur

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.filter.color.delinearize
import org.openrndr.filter.color.linearize

class BloomDownscale : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("blur/bloom-downscale.frag"))) {

}

class BloomUpscale : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("blur/bloom-upscale.frag"))) {
    var gain:Double by parameters
    var shape:Double by parameters
    var seed:Double by parameters

    init {
        gain = 1.0
        shape = 1.0
        seed = 1.0
    }
}

class BloomCombine: Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("blur/bloom-combine.frag"))) {
    var gain: Double by parameters
    var bias: ColorRGBa by parameters

    init {
        bias = ColorRGBa.BLACK
        gain = 1.0
    }
}

@Description("MipBloom")
open class MipBloom<T:Filter>(val blur:T) : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("blur/bloom-combine.frag"))) {
    var passes = 6

    @DoubleParameter("shape", 0.0, 4.0)
    var shape: Double = 1.0

    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double = 1.0

    @BooleanParameter("sRGB")
    var sRGB = true

    var intermediates = mutableListOf<ColorBuffer>()
    var sourceCopy: ColorBuffer? = null

    val upscale = BloomUpscale()
    val downScale = BloomDownscale()
    val combine = BloomCombine()

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {

        sourceCopy?.let {
            if (it.width != source[0].width || it.height != source[0].height) {
                it.destroy()
                sourceCopy = null
            }
        }

        if (sourceCopy == null) {
            sourceCopy = colorBuffer(source[0].width, source[0].height, type = ColorType.FLOAT16)
        }

        source[0].copyTo(sourceCopy!!)

        upscale.shape = shape
        if (intermediates.size != passes
                || (intermediates.isNotEmpty() && (intermediates[0].width!=target[0].width || intermediates[0].height != target[0].height) )) {
            intermediates.forEach {
                it.destroy()
            }
            intermediates.clear()

            for (pass in 0 until passes) {
                val tdiv = 1 shl (pass+1)
                val cb = colorBuffer(target[0].width / tdiv, target[0].height / tdiv, type = ColorType.FLOAT16)
                intermediates.add(cb)
            }
        }


        if (sRGB) {
            linearize.apply(sourceCopy!!, sourceCopy!!)
        }

        downScale.apply(sourceCopy!!, intermediates[0])
        blur.apply(intermediates[0], intermediates[0])

        for (pass in 1 until passes) {
            downScale.apply(intermediates[pass-1], intermediates[pass])
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