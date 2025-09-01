@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur


import org.openrndr.draw.Filter2to1
import org.openrndr.extra.fx.fx_directional_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

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
     * The sample window: how many samples to mix. The default is 5
     */
    @IntParameter("window size", 1, 25)
    var window: Int by parameters

    /**
     * Spread multiplier: the distance in pixels between sampled pixels. The default is 1.0
     */
    @DoubleParameter("kernel spread", 1.0, 4.0)
    var spread: Double by parameters

    /**
     * Post-blur gain, default is 1.0
     */
    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double by parameters

    /**
     * Should filter use directions perpendicular to those in the direction buffer? default is false
     */
    @BooleanParameter("perpendicular")
    var perpendicular: Boolean by parameters

    /**
     * Wrap around left and right edges
     */
    @BooleanParameter("wrapX")
    var wrapX: Boolean by parameters

    /**
     * Wrap around top and bottom edges
     */
    @BooleanParameter("wrapY")
    var wrapY: Boolean by parameters

    init {
        window = 5
        spread = 1.0
        gain = 1.0
        perpendicular = false
        centerWindow = false
        wrapX = false
        wrapY = false
    }
}