package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_fisheye
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Fisheye")
class Fisheye : Filter(mppFilterShader(fx_fisheye, "fisheye")) {
    @DoubleParameter("strength", -1.0, 1.0, order = 0)
    var strength: Double by parameters

    @DoubleParameter("scale", 0.0, 2.0, order = 0)
    var scale: Double by parameters

    @DoubleParameter("feather", 0.0, 100.0, order = 1)
    var feather: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0, order = 1)
    var rotation: Double by parameters

    init {
        strength = 0.1
        feather = 1.0
        scale = 1.0
        rotation = 0.0
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
