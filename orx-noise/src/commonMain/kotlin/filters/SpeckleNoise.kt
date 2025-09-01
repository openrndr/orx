@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.noise.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.shaderphrases.preprocess

/**
 * Speckle noise filter
 */
@Description("Speckle Noise")
class SpeckleNoise : Filter(filterShaderFromCode(run {
    noise_speckle.preprocess()
}, "speckle-noise")) {

    /**
     * The color of the generated speckles
     */
    @ColorParameter("Color")
    var color: ColorRGBa by parameters

    /**
     * Density of the speckles, default is 0.1, min, 0.0, max is 1.0
     */
    @DoubleParameter("Density", 0.0, 1.0)
    var density: Double by parameters

    /**
     * Noisiness of the generated speckles, default is 0.0, min is 0.0, max is 1.0
     */
    @DoubleParameter("Noise", 0.0, 1.0)
    var noise: Double by parameters

    /**
     * should the output colors be multiplied by the alpha channel, default is true
     */
    var premultipliedAlpha: Boolean by parameters

    /**
     * noise seed, feed it with time to animate
     */
    @DoubleParameter("Seed", 0.0, 1000.0)
    var seed: Double by parameters

    init {
        density = 0.1
        color = ColorRGBa.WHITE
        seed = 0.0
        noise = 0.0
        premultipliedAlpha = true
    }
}