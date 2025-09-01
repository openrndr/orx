@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.distort

import org.openrndr.draw.Filter2to1
import org.openrndr.extra.fx.fx_directional_displace
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

/**
 * Directional displace filter. Takes source image and direction buffer inputs
 */
@Description("Directional displace")
class DirectionalDisplace : Filter2to1(mppFilterShader(fx_directional_displace, "directional-displace")) {

    /**
     * The distance of the sampled pixel. The default is 1.0
     */
    @DoubleParameter("distance", 1.0, 1000.0)
    var distance: Double by parameters

    /**
     * Post-displace gain, default is 1.0
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
        distance = 1.0
        gain = 1.0
        perpendicular = false
        wrapX = false
        wrapY = false
    }
}