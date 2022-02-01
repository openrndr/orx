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
import org.openrndr.extra.parameters.IntParameter

@Description("Lenses")
class Lenses : Filter(mppFilterShader(fx_lenses, "block-repeat")) {
    @IntParameter("rows", 1, 64, order = 0)
    var rows: Int by parameters

    @IntParameter("columns", 1, 64, order = 1)
    var columns: Int by parameters

    @DoubleParameter("scale", 0.5, 1.5, order = 2)
    var scale: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0, order = 3)
    var rotation: Double by parameters

    @DoubleParameter("distort", -1.0, 1.0, order = 4)
    var distort: Double by parameters

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
        rows = 6
        columns = 8
        distort = 0.0

        bicubicFiltering = true
    }
}
