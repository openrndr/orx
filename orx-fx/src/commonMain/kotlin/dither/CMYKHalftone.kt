package org.openrndr.extra.fx.dither

import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_cmyk_halftone
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("CMYK Halftone")
class CMYKHalftone: Filter1to1(mppFilterShader(fx_cmyk_halftone, "cmyk-halftone")) {
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