package org.openrndr.extra.fx.blur

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Frame blur")
class FrameBlur : Filter(filterShaderFromUrl(filterFragmentUrl("blur/frame-blur.frag"))) {

    @DoubleParameter("blend", 0.0, 1.0)
    var blend: Double by parameters

    private var intermediate: ColorBuffer? = null

    init {
        blend = 0.5
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (target.isNotEmpty()) {
            intermediate?.let {
                if (it.width != target[0].width || it.height != target[0].height) {
                    it.destroy()
                    intermediate = null
                }

            }

            if (intermediate == null) {
                intermediate = colorBuffer(target[0].width, target[0].height, type = ColorType.FLOAT16)
                intermediate?.fill(ColorRGBa.TRANSPARENT)
            }

            super.apply(arrayOf(source[0], intermediate!!), arrayOf(intermediate!!))
            intermediate!!.copyTo(target[0])
        }
    }

}
