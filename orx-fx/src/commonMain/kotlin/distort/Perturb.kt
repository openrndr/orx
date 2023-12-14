@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_perturb
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

@Description("Perturb")
class Perturb : Filter1to1(mppFilterShader(fx_perturb, "perturb")) {
    var seed: Vector3 by parameters
    /**
     * base noise scale, default is Vector3(1.0, 1.0, 1.0)
     */
    @DoubleParameter("scale", 0.01, 8.0, order = 0)
    var scale: Double by parameters

    @DoubleParameter("phase", -2.0, 2.0, order = 1)
    var phase: Double by parameters

    @DoubleParameter("radius", 0.0, 2.0, order = 1)
    var radius: Double by parameters

    /**
     * lacunarity is the amount by which scale is modulated per octave, default is Vector3(2.0, 2.0, 2.0)
     */
    @DoubleParameter("lacunarity", 0.0, 1.0, order = 2)
    var lacunarity: Double by parameters

    @DoubleParameter("gain", 0.0, 1.0, order = 3)
    var gain: Double by parameters

    @DoubleParameter("decay", 0.0, 1.0, order = 4)
    var decay: Double by parameters

    /**
     * the number of octaves of noise to generate, default is 4
     */
    @IntParameter("octaves", 1, 10, order = 5)
    var octaves: Int by parameters

    @IntParameter("x segments", 0, 256, order = 6)
    var xSegments: Int by parameters

    @IntParameter("y segments", 0, 256, order = 7)
    var ySegments: Int by parameters

    @BooleanParameter("output UV", order = 8)
    var outputUV: Boolean by parameters

    @Vector2Parameter("offset", -1.0, 1.0, order = 9)
    var offset: Vector2 by parameters


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
        outputUV = false
        offset = Vector2.ZERO
        radius = 1.0

    }
    var bicubicFiltering = true
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target, clip)
    }
}