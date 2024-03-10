@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_lenses
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.shape.Rectangle

@Description("Lenses")
class Lenses : Filter1to1(mppFilterShader(fx_lenses, "lenses")) {
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

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target, clip)
    }

    init {
        rows = 6
        columns = 8
        distort = 0.0
        scale = 1.0
        rotation = 0.0
        bicubicFiltering = true
    }
}
