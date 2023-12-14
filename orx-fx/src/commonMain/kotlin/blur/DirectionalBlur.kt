@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_directional_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.shape.Rectangle

/**
 * Directional blur filter. Takes source image and direction buffer inputs
 */
@Description("Directional blur")
class DirectionalBlur : Filter2to1(mppFilterShader(fx_directional_blur, "directional-blur")) {

    /**
     * Should the blur window be centered, default is false
     */
    @BooleanParameter("center window")
    var centerWindow: Boolean by parameters

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

    /**
     * Should filter use directions perpendicular to those in the direction buffer?
     */
    @BooleanParameter("perpendicular")
    var perpendicular: Boolean by parameters



    init {
        window = 5
        spread = 1.0
        gain = 1.0
        perpendicular = false
        centerWindow = false
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        parameters["wrapX"] = false
        parameters["wrapY"] = false
        super.apply(source, target, clip)
    }
}