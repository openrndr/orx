package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

@Description("Perturb")
class Perturb : Filter(filterShaderFromUrl(filterFragmentUrl("distort/perturb.frag"))) {
    var seed: Vector3 by parameters

    /**
     * base noise scale, default is Vector3(1.0, 1.0, 1.0)
     */
    @DoubleParameter("scale", 0.01, 8.0)
    var scale: Double by parameters

    @DoubleParameter("phase", -2.0, 2.0)
    var phase: Double by parameters

    /**
     * lacunarity is the amount by which scale is modulated per octave, default is Vector3(2.0, 2.0, 2.0)
     */
    @DoubleParameter("lacunarity", 0.0, 1.0)
    var lacunarity: Double by parameters

    @DoubleParameter("gain", 0.0, 1.0)
    var gain: Double by parameters

    @DoubleParameter("decay", 0.0, 1.0)
    var decay: Double by parameters

    /**
     * the number of octaves of noise to generate, default is 4
     */
    @IntParameter("octaves", 1, 10)
    var octaves: Int by parameters


    @IntParameter("x segments", 0, 256)
    var xSegments: Int by parameters

    @IntParameter("y segments", 0, 256)
    var ySegments: Int by parameters



    init {
        seed = Vector3.ZERO
        scale = 1.0
        lacunarity = 2.0
        gain = 0.5
        decay = 0.5
        octaves = 4
        phase = 0.0
        xSegments = 0
        ySegments = 0

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