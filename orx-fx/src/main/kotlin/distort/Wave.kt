package org.openrndr.extra.fx.distort

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Horizontal wave")
class HorizontalWave : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("distort/horizontal-wave.frag"))) {
    @DoubleParameter("frequency", 0.0, 64.0)
    var frequency: Double by parameters

    @DoubleParameter("amplitude", 0.0, 1.0)
    var amplitude: Double by parameters

    @DoubleParameter("phase", -0.5, 0.5)
    var phase: Double by parameters

    init {
        frequency = 1.0
        amplitude = 0.1
        phase = 0.0
    }
}

@Description("Vertical wave")
class VerticalWave : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("distort/vertical-wave.frag"))) {
    @DoubleParameter("frequency", 0.0, 64.0)
    var frequency: Double by parameters

    @DoubleParameter("amplitude", 0.0, 1.0)
    var amplitude: Double by parameters

    @DoubleParameter("phase", -0.5, 0.5)
    var phase: Double by parameters

    init {
        frequency = 1.0
        amplitude = 0.1
        phase = 0.0
    }

}