package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_displace_blend
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector3

@Description("Displace blend")
class DisplaceBlend : Filter(mppFilterShader(fx_displace_blend, "displace-blend")) {
    var seed: Vector3 by parameters

    @DoubleParameter("offset", -1.0, 1.0)
    var offset: Double by parameters

    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double by parameters

    @DoubleParameter("feather", 1.0, 100.0)
    var feather: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0)
    var rotation: Double by parameters

    @DoubleParameter("source opacity", 0.0, 1.0)
    var sourceOpacity: Double by parameters

    @DoubleParameter("target opacity", 0.0, 1.0)
    var targetOpacity: Double by parameters

    init {
        gain = 0.1
        offset = 0.5
        rotation = 0.0
        feather = 1.0
        sourceOpacity = 1.0
        targetOpacity = 1.0
    }

    var bicubicFiltering = true
    private var intermediate: ColorBuffer? = null
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (source.size >= 2) {
            if (target[0] === source[0] || target[0] === source[1]) {
                if (intermediate == null) {
                    intermediate = colorBuffer(target[0].width, target[0].height, type = target[0].type, format = target[0].format)
                }
            }
            if (bicubicFiltering && source.isNotEmpty()) {
                source[0].generateMipmaps()
                source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
            }
            super.apply(source, arrayOf(intermediate ?: target[0]))
            intermediate?.copyTo(target[0])
        }
    }
}