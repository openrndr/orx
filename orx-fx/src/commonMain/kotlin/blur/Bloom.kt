package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.fx_bloom
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

@Description("Bloom")
class Bloom(blur: Filter = ApproximateGaussianBlur()) : Filter1to1(mppFilterShader(fx_bloom, "bloom")) {
    /**
     * the blur filter to use for the bloom, default is Approximate Gaussian Blur
     */
    var blur: Filter = blur

    /**
     * number of downsampled textures to use, default value is 2
     */
    @IntParameter("blur iterations", 1, 8)
    var downsamples: Int = 2

    /**
     * rate of downsampling, f.ex: 4 -> 4x, 8x, 16x.., default value is 2
     */
    @IntParameter("downsamping rate", 1, 4)
    var downsampleRate: Int = 2

    /**
     * blending amount between original image and blurred, default value is 0.5
     */
    @DoubleParameter("blend factor", 0.0, 1.0)
    var blendFactor: Double by parameters

    /**
     * brightness of the resulting image, default value is 0.5
     */
    @DoubleParameter("brightness", 0.0, 1.0)
    var brightness: Double by parameters

    init {
        blendFactor = 0.5
        brightness = 0.5
    }

    private val samplers: MutableList<Pair<ColorBuffer, ColorBuffer>> = mutableListOf()
    private var lastDownsampleRate = 0

    private val blendAdd = Add()

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        val src = source[0]
        val dest = target[0]

        if (samplers.isEmpty() || samplers.size != downsamples || lastDownsampleRate != downsampleRate) {
            samplers.clear()

            lastDownsampleRate = downsampleRate

            for (downsample in 0 until downsamples) {
                val div = 1 shl downsample
                val bufferA = colorBuffer(dest.width / div, dest.height / div, 1.0, target[0].format, ColorType.FLOAT16)
                val bufferB = colorBuffer(dest.width / div, dest.height / div, 1.0, target[0].format, ColorType.FLOAT16)
                samplers.add(Pair(bufferA, bufferB))
            }
        }

        for ((bufferA, _) in samplers) {
            blur.apply(src, bufferA)
        }

        for ((index, buffers) in samplers.asReversed().withIndex()) {
            val (bufferCurrA) = buffers

            if (index + 1 <= samplers.lastIndex) {
                val (bufferNextA, bufferNextB) = samplers.asReversed()[index + 1]

                blur.apply(bufferCurrA, bufferNextB)
                blendAdd.apply(arrayOf(bufferNextA, bufferNextB), bufferNextA)
            } else {
                super.apply(arrayOf(src, bufferCurrA), target)
            }
        }
    }
}