package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_box_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

import org.openrndr.math.Vector2
import org.openrndr.math.asRadians
import kotlin.math.cos
import kotlin.math.sin

/**
 * BoxBlur implemented as a separable filter
 */
@Description("Line blur")
class LineBlur : Filter(mppFilterShader(fx_box_blur, "line-blur")) {

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


    @DoubleParameter("blur angle", -180.0, 180.0)
    var blurAngle: Double by parameters

    @BooleanParameter("wrap x", order = 9)
    var wrapX: Boolean by parameters

    @BooleanParameter("wrap y", order = 10)
    var wrapY: Boolean by parameters



    init {
        window = 5
        spread = 1.0
        gain = 1.0
        blurAngle = 0.0
        wrapX = false
        wrapY = false
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        parameters["blurDirection"] = Vector2(cos(blurAngle.asRadians), sin(blurAngle.asRadians))
        super.apply(source, target)
    }
}