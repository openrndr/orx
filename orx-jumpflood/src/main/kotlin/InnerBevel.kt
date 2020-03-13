package org.openrndr.extra.jumpfill

import org.openrndr.draw.*
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl

private class InnerBevelFilter : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/fx/inner-bevel.frag"))) {
    var angle: Double by parameters
    var width: Double by parameters

    var noise:Double by parameters
    init {
        angle = 0.0
        width = 5.0
        noise = 0.0
    }
}

@Description("Inner bevel")
class InnerBevel : Filter() {
    @DoubleParameter("threshold", 0.0, 1.0)
    var threshold = 0.01

    @DoubleParameter("distance scale", 0.0, 1.0)
    var distanceScale = 1.0

    @DoubleParameter("angle", -180.0, 180.0)
    var angle = 0.0

    @DoubleParameter("width", 0.0, 50.0)
    var width = 5.0

    @DoubleParameter("noise", 0.0, 1.0)
    var noise = 0.1

    private var jumpFlooder: JumpFlooder? = null
    private val decodeFilter = PixelDirection()
    private val bevelFilter = InnerBevelFilter()

    private var distance: ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (jumpFlooder == null) {
            jumpFlooder = JumpFlooder(target[0].width, target[0].height, encodePoints = EncodeSubpixel())
        }
        if (distance == null) {
            distance = colorBuffer(target[0].width, target[0].height, type = ColorType.FLOAT32)
        }
        val result = jumpFlooder!!.jumpFlood(source[0])
        decodeFilter.originalSize = Vector2(target[0].width * 1.0, target[0].height * 1.0)
        decodeFilter.distanceScale = distanceScale
        decodeFilter.apply(result, result)
        result.copyTo(distance!!)
        bevelFilter.angle = angle
        bevelFilter.width = width
        bevelFilter.noise = noise
        bevelFilter.apply(arrayOf(source[0], distance!!), target[0])
    }
}