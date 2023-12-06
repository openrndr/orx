@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.dither

import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_luma_halftone
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter


@Description("Luma Halftone")
class LumaHalftone: Filter1to1(filterShaderFromCode(fx_luma_halftone, "luma-halftone")) {
    @DoubleParameter("scale", 1.0, 30.0, precision = 4)
    var scale: Double by parameters

    @DoubleParameter("threshold", 0.0, 1.0, precision = 4)
    var threshold: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0)
    var rotation: Double by parameters

    @DoubleParameter("freq0", 1.0, 400.0)
    var freq0: Double by parameters

    @DoubleParameter("freq1", 1.0, 400.0)
    var freq1: Double by parameters

    @DoubleParameter("gain1", -2.0, 2.0)
    var gain1: Double by parameters

    @DoubleParameter("phase0", -1.0, 1.0)
    var phase0: Double by parameters

    @DoubleParameter("phase1", -1.0, 1.0)
    var phase1: Double by parameters


    @BooleanParameter("invert")
    var invert: Boolean by parameters


    init {
        scale = 3.0
        rotation = 0.0
        threshold = 0.5
        freq1 = 20.0
        freq0 = 10.0
        gain1 = 0.1
        phase0 = 0.0
        phase1 = 0.0
        invert = true
    }
}