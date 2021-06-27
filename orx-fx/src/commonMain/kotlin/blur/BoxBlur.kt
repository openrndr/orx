package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_box_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

import org.openrndr.math.Vector2

/**
 * BoxBlur implemented as a separable filter
 */
@Description("Box-blur")
class BoxBlur : Filter(mppFilterShader(fx_box_blur,"box-blur")) {

    data class ColorBufferDescription(val width: Int, val height: Int, val contentScale: Double, val format: ColorFormat, val type: ColorType)

    /**
     * The sample window, default is 5
     */
    @IntParameter("window size", 1, 25)
    var window: Int by parameters

    /**
     * Spread multiplier, default is 1.0
     */
    @DoubleParameter("kernel spread", 1.0, 4.0)
    var spread: Double by parameters

    /**
     * Post-blur gain, default is 1.0
     */
    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double by parameters

    private var intermediateCache = mutableMapOf<ColorBufferDescription, ColorBuffer>()

    init {
        window = 5
        spread = 1.0
        gain = 1.0
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