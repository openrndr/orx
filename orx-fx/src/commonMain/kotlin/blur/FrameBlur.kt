@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough
import org.openrndr.extra.fx.fx_frame_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.shape.Rectangle

@Description("Frame blur")
class FrameBlur(val colorType: ColorType = ColorType.FLOAT16) :
    Filter1to1(mppFilterShader(fx_frame_blur, "frame-blur")) {

    @DoubleParameter("blend", 0.0, 1.0)
    var blend: Double by parameters


    val pt = Passthrough()
    private var intermediate: ColorBuffer? = null
    private var intermediate2: ColorBuffer? = null

    init {
        blend = 0.5
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)
        if (source.isNotEmpty() && target.isNotEmpty()) {
            intermediate?.let {
                if (it.isEquivalentTo(target[0], ignoreFormat = true, ignoreLevels = true)) {
                    it.destroy()
                    intermediate = null
                }
            }
            intermediate2?.let {
                if (it.isEquivalentTo(target[0], ignoreFormat = true, ignoreLevels = true)) {
                    it.destroy()
                    intermediate2 = null
                }
            }

            if (intermediate == null) {
                intermediate = target[0].createEquivalent(type = colorType)
                intermediate?.fill(ColorRGBa.TRANSPARENT)
            }
            if (intermediate2 == null) {
                intermediate2 = target[0].createEquivalent(type = colorType)
                intermediate2?.fill(ColorRGBa.TRANSPARENT)
            }

            super.apply(arrayOf(source[0], intermediate!!), arrayOf(intermediate2!!), clip)

            pt.apply(intermediate2!!, intermediate!!)
            pt.apply(intermediate!!, target[0])
        }
    }
}
