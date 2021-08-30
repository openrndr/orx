package org.openrndr.extra.fx.distort

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.extra.fx.fx_polar_to_rectangular
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2

@Description("Polar to rectangular")
class PolarToRectangular : Filter(mppFilterShader(fx_polar_to_rectangular, "polar-to-rectangular")) {
    var bicubicFiltering = true
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target)
    }
}