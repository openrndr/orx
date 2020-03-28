package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

@Description("Horizontal wave")
class HorizontalWave : Filter(filterShaderFromUrl(filterFragmentUrl("distort/horizontal-wave.frag"))) {
    @DoubleParameter("frequency", 0.0, 64.0, order = 1)
    var frequency: Double by parameters

    @DoubleParameter("amplitude", 0.0, 1.0, order = 0)
    var amplitude: Double by parameters

    @DoubleParameter("phase", -0.5, 0.5, order = 2)
    var phase: Double by parameters

    @IntParameter("segments", 0, 256, order = 3)
    var segments: Int by parameters


    init {
        frequency = 1.0
        amplitude = 0.1
        phase = 0.0
        segments = 0
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

@Description("Vertical wave")
class VerticalWave : Filter(filterShaderFromUrl(filterFragmentUrl("distort/vertical-wave.frag"))) {
    @DoubleParameter("frequency", 0.0, 64.0, order = 1)
    var frequency: Double by parameters

    @DoubleParameter("amplitude", 0.0, 1.0, order = 0)
    var amplitude: Double by parameters

    @DoubleParameter("phase", -0.5, 0.5, order = 2)
    var phase: Double by parameters

    @IntParameter("segments", 0, 256, order = 3)
    var segments: Int by parameters


    init {
        frequency = 1.0
        amplitude = 0.1
        phase = 0.0
        segments = 0
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