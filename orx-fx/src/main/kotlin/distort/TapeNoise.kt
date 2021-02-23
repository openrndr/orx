package org.openrndr.extra.fx.distort


import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.ColorParameter
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

    @DoubleParameter("gap frequency", 0.0, 2.0)
    var gapFrequency: Double by parameters

    @DoubleParameter("gap low", -1.0, 1.0)
    var gapLow: Double by parameters
    @DoubleParameter("gap high", -1.0, 1.0)
    var gapHigh: Double by parameters

    @DoubleParameter("deform amplitude", 0.0, 1.0)
    var deformAmplitude: Double by parameters

    @DoubleParameter("deform frequency", 0.0, 1.0)
    var deformFrequency: Double by parameters


    @ColorParameter("tint")
    var tint: ColorRGBa by parameters

    @BooleanParameter("monochrome")
    var monochrome: Boolean by parameters

    init {
        gain = 0.5
        noiseLow = 0.5
        noiseHigh = 0.8
        tint = ColorRGBa.WHITE
        monochrome = false
        gapFrequency = 10.0
        gapLow = -1.0
        gapHigh = -0.99

     }
}