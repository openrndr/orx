package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_polar_to_rectangular
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2

@Description("Polar to rectangular")
class PolarToRectangular : Filter1to1(mppFilterShader(fx_polar_to_rectangular, "polar-to-rectangular")) {
    @BooleanParameter("log polar")
    var logPolar:Boolean by parameters

    init {
        logPolar = true
    }


    var bicubicFiltering = true
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target)
    }
}