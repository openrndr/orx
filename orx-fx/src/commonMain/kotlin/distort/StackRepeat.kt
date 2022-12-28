package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_stack_repeat
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

@Description("Stack repeat")
class StackRepeat : Filter1to1(mppFilterShader(fx_stack_repeat, "stack-repeat")) {
    @DoubleParameter("zoom", -1.0, 1.0, order = 0)
    var zoom: Double by parameters

    @DoubleParameter("x-origin", -1.0, 1.0, order = 1)
    var xOrigin: Double by parameters

    @DoubleParameter("y-origin", -1.0, 1.0, order = 2)
    var yOrigin: Double by parameters

    @DoubleParameter("x-offset", -1.0, 1.0, order = 3)
    var xOffset: Double by parameters

    @DoubleParameter("y-offset", -1.0, 1.0, order = 4)
    var yOffset: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0, order = 5)
    var rotation: Double by parameters

    @IntParameter("repeats", 0, 16, order = 6)
    var repeats: Int by parameters

    init {
        zoom = 0.0
        repeats = 2
        xOffset = 0.0
        yOffset = 0.0
        xOrigin = 0.0
        yOrigin = 0.0
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
