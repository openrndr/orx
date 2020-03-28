package org.openrndr.extra.fx.dither

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("CMYK Halftone")
class CMYKHalftone: Filter(filterShaderFromUrl(filterFragmentUrl("dither/cmyk-halftone.frag"))) {

    @DoubleParameter("scale", 1.0, 30.0, precision = 4)
    var scale: Double by parameters

    @DoubleParameter("dotSize", 1.0, 3.0, precision = 4)
    var dotSize: Double by parameters


    @DoubleParameter("rotation", -180.0, 180.0)
    var rotation: Double by parameters

    init {
        scale = 3.0
        rotation = 0.0
        dotSize = 1.0
    }
}