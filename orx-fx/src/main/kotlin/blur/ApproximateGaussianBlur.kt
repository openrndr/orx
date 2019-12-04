package org.openrndr.extra.fx.blur

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.fx.filterFragmentCode

import org.openrndr.math.Vector2

/**
 * Approximate separated Gaussian blur
 */
class ApproximateGaussianBlur : Filter(Shader.createFromCode(Filter.filterVertexCode,
        filterFragmentCode("blur/approximate-gaussian-blur.frag"))) {

    /**
     * blur sample window, default value is 5
     */
    var window: Int by parameters

    /**
     * spread multiplier, default value is 1.0
     */
    var spread: Double by parameters

    /**
     * blur sigma, default value is 1.0
     */
    var sigma: Double by parameters

    /**
     * post blur gain, default value is 1.0
     */
    var gain: Double by parameters


    private var intermediate: ColorBuffer? = null

    init {
        window = 5
        spread = 1.0
        gain = 1.0
        sigma = 1.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        intermediate?.let {
            if (it.width != target[0].width || it.height != target[0].height) {
                intermediate = null
            }
        }

        if (intermediate == null) {
            intermediate = colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }

        intermediate?.let {
            parameters["blurDirection"] = Vector2(1.0, 0.0)
            super.apply(source, arrayOf(it))

            parameters["blurDirection"] = Vector2(0.0, 1.0)
            super.apply(arrayOf(it), target)
        }
    }
}