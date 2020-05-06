package org.openrndr.extra.fx.distort


import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter


@Description("Tape noise")
class TapeNoise : Filter(filterShaderFromUrl(filterFragmentUrl("distort/tape-noise.frag"))) {
    var time: Double by parameters

    @DoubleParameter("gain", 0.0, 1.0)
    var gain: Double by parameters

    @DoubleParameter("noise low", 0.0, 1.0)
    var noiseLow: Double by parameters

    @DoubleParameter("noise high", 0.0, 1.0)
    var noiseHigh: Double by parameters

    init {
        gain = 0.5
        noiseLow = 0.5
        noiseHigh = 0.8
    }
}