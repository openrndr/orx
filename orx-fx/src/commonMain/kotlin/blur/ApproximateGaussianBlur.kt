package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.fx.ColorBufferDescription
import org.openrndr.extra.fx.fx_approximate_gaussian_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

import org.openrndr.math.Vector2

/**
 * Approximate separated Gaussian blur
 */
@Description("Approximate Gaussian blur")
class ApproximateGaussianBlur : Filter(mppFilterShader(fx_approximate_gaussian_blur, "approximate gaussian blur")) {
    /**
     * blur sample window, default value is 5
     */
    @IntParameter("window size", 1, 25)
    var window: Int by parameters

    /**
     * spread multiplier, default value is 1.0
     */
    @DoubleParameter("kernel spread", 1.0, 4.0)
    var spread: Double by parameters

    /**
     * blur sigma, default value is 1.0
     */
    @DoubleParameter("kernel sigma", 0.0, 25.0)
    var sigma: Double by parameters

    /**
     * post blur gain, default value is 1.0
     */
    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double by parameters

    private var intermediateCache = mutableMapOf<ColorBufferDescription, ColorBuffer>()

    init {
        window = 5
        spread = 1.0
        gain = 1.0
        sigma = 1.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        val intermediateDescription = ColorBufferDescription(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        val intermediate = intermediateCache.getOrPut(intermediateDescription) {
            colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }

        intermediate.let {
            parameters["blurDirection"] = Vector2(1.0, 0.0)
            super.apply(source, arrayOf(it))

            parameters["blurDirection"] = Vector2(0.0, 1.0)
            super.apply(arrayOf(it), target)
        }
    }
}