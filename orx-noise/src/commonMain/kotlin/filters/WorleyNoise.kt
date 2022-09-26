package org.openrndr.extra.noise.filters

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.parameters.*
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.math.Vector2

/**
 * Filter for Worley Noise
 */
@Description("Worley Noise")
class WorleyNoise : Filter(filterShaderFromCode(run {
    noise_worley.preprocess()
}, "worley-noise")) {
    @DoubleParameter("Scale", 0.1, 200.0)
    var scale: Double by parameters

    @BooleanParameter("Premultiplied alpha")
    var premultipliedAlpha: Boolean by parameters

    @Vector2Parameter("Offset")
    var offset: Vector2 by parameters

    init {
        premultipliedAlpha = true
        scale = 5.0
        offset = Vector2.ZERO
    }
}