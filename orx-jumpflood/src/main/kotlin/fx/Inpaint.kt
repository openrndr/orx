package org.openrndr.extra.jumpfill.fx

import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.EncodeSubpixel
import org.openrndr.extra.jumpfill.JumpFlooder
import org.openrndr.extra.jumpfill.PixelDirection
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl

private class InpaintFilter : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/fx/inpaint.frag"))) {

    var noise: Double by parameters
    var imageOpacity: Double by parameters
    var opacity : Double by parameters
    var shape : Double by parameters
    var width: Double by parameters
    init {
        noise = 0.0
        imageOpacity = 1.0
        opacity = 1.0
        shape = 0.0
        width = 0.5
    }
}

@Description("Inpaint")
class Inpaint : Filter() {
    @DoubleParameter("width", 0.0, 1.0)
    var width = 0.5

    @DoubleParameter("noise", 0.0, 1.0)
    var noise = 0.1


    @DoubleParameter("opacity", 0.0, 1.0)
    var opacity = 1.0

    @DoubleParameter("image opacity", 0.0, 1.0)
    var imageOpacity = 1.0

    @DoubleParameter("shape", 0.0, 10.0)
    var shape = 0.0


    private var jumpFlooder: JumpFlooder? = null
    private val decodeFilter = PixelDirection()
    private val inpaintFilter = InpaintFilter()

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
        inpaintFilter.noise = noise
        inpaintFilter.imageOpacity = imageOpacity
        inpaintFilter.opacity = opacity
        inpaintFilter.shape = shape
        inpaintFilter.width = width
        inpaintFilter.apply(arrayOf(source[0], distance!!), target[0])
    }
}