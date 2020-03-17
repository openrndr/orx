package org.openrndr.extra.jumpfill.fx

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.EncodeSubpixel
import org.openrndr.extra.jumpfill.JumpFlooder
import org.openrndr.extra.jumpfill.PixelDirection
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl

private class InnerGlowFilter : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/fx/inner-glow.frag"))) {
    var angle: Double by parameters
    var width: Double by parameters

    var noise: Double by parameters
    var color: ColorRGBa by parameters

    var shape: Double by parameters
    var imageOpacity: Double by parameters

    init {
        angle = 0.0
        width = 5.0
        noise = 0.0
        shape = 1.0
        imageOpacity = 1.0
    }
}

@Description("Inner glow")
class InnerGlow : Filter() {
    @DoubleParameter("width", 0.0, 50.0)
    var width = 5.0

    @DoubleParameter("noise", 0.0, 1.0)
    var noise = 0.1

    @DoubleParameter("shape", 0.0, 10.0)
    var shape = 1.0

    @DoubleParameter("opacity", 0.0, 1.0)
    var opacity = 1.0

    @DoubleParameter("image opacity", 0.0, 1.0)
    var imageOpacity = 1.0


    @ColorParameter("color")
    var color = ColorRGBa.WHITE

    private var jumpFlooder: JumpFlooder? = null
    private val decodeFilter = PixelDirection()
    private val glowFilter = InnerGlowFilter()

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
        decodeFilter.distanceScale = 1.0
        decodeFilter.apply(result, result)
        result.copyTo(distance!!)
        glowFilter.color = color.opacify(opacity)
        glowFilter.width = width
        glowFilter.noise = noise
        glowFilter.shape = shape
        glowFilter.imageOpacity = imageOpacity
        glowFilter.apply(arrayOf(source[0], distance!!), target[0])
    }
}