package org.openrndr.extra.fx.distort

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.extra.fx.fx_rectangular_to_polar
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2
import kotlin.math.log

@Description("Rectangular to polar")
class RectangularToPolar : Filter(mppFilterShader(fx_rectangular_to_polar, "rectangular-to-polar")) {

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