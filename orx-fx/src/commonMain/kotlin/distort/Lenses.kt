package org.openrndr.extra.fx.distort

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.extra.fx.fx_lenses
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Lenses")
class Lenses : Filter(mppFilterShader(fx_lenses, "block-repeat")) {
    @DoubleParameter("block width", 0.0, 1.0, order = 0)
    var blockWidth: Double by parameters

    @DoubleParameter("block height", 0.0, 1.0, order = 1)
    var blockHeight: Double by parameters


    @DoubleParameter("scale", 0.5, 1.5, order = 2)
    var scale: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0, order = 3)
    var rotation: Double by parameters

    @BooleanParameter("bicubic filtering")
    var bicubicFiltering: Boolean by parameters

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target)
    }

    init {
        blockWidth = 0.25
        blockHeight = 0.25

        bicubicFiltering = true
    }
}
