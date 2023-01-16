package org.openrndr.extra.fx.blur

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_frame_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Frame blur")
class FrameBlur(val colorType: ColorType = ColorType.FLOAT16) :
    Filter1to1(mppFilterShader(fx_frame_blur, "frame-blur")) {

    @DoubleParameter("blend", 0.0, 1.0)
    var blend: Double by parameters

    private var intermediate: ColorBuffer? = null

    init {
        blend = 0.5
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (source.isNotEmpty() && target.isNotEmpty()) {
            intermediate?.let {
                if (it.isEquivalentTo(target[0], ignoreFormat = true, ignoreLevels = true)) {
                    it.destroy()
                    intermediate = null
                }
            }

            if (intermediate == null) {
                intermediate = target[0].createEquivalent(type = colorType)
                intermediate?.fill(ColorRGBa.TRANSPARENT)
            }

            super.apply(arrayOf(source[0], intermediate!!), arrayOf(intermediate!!))
            intermediate!!.copyTo(target[0])
        }
    }
}
