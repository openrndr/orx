package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Stretch waves")
class StretchWaves : Filter(filterShaderFromCode(filterFragmentCode("distort/stretch-waves.frag"))) {
    @DoubleParameter("distortion", -0.0,1.0, 1)
    var distortion: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0)
    var rotation: Double by parameters

    @DoubleParameter("phase", -1.0, 1.0)
    var phase: Double by parameters

    @DoubleParameter("frequency", 0.0, 100.0)
    var frequency: Double by parameters

    @DoubleParameter("feather", 0.0, 100.0, order = 1)
    var feather: Double by parameters

    init {
        distortion = 0.0
        rotation = 0.0
        phase = 0.0
        frequency = 10.0
        feather = 1.0
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