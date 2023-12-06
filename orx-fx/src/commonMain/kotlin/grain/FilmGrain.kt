@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.grain

import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_film_grain
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
/**
 * Film grain filter
 */
@Description("film grain")
class FilmGrain : Filter1to1(mppFilterShader(fx_film_grain, "film-grain")) {
    @BooleanParameter("use color")
    var useColor: Boolean by parameters

    var time: Double by parameters;

    @DoubleParameter("grain lift ratio", 0.0, 1.0)
    var grainLiftRatio: Double by parameters

    @DoubleParameter("grain strength", 0.0, 1.0)
    var grainStrength: Double by parameters

    @DoubleParameter("grain rate", 0.0, 1.0)
    var grainRate: Double by parameters

    @DoubleParameter("grain pitch", 0.0, 1.0)
    var grainPitch: Double by parameters

    @DoubleParameter("color level", 0.0, 1.0)
    var colorLevel: Double by parameters

    init {
        useColor = false
        grainLiftRatio = 0.5
        grainStrength = 1.0
        grainRate = 1.0
        grainPitch = 1.0
        colorLevel = 1.0
    }
}