@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_zoom_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

@Description("Zoom Blur")
class ZoomBlur : Filter1to1(mppFilterShader(fx_zoom_blur, "zoom-blur")) {
    var center: Vector2 by parameters

    @DoubleParameter("strength", 0.0, 1.0)
    var strength: Double by parameters

    init {
        center = Vector2.ONE / 2.0
        strength = 0.2
    }

    private var intermediate: ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        intermediate?.let {
            if (it.width != target[0].width || it.height != target[0].height) {
                intermediate = null
            }
        }

        if (intermediate == null) {
            intermediate =
                colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }

        intermediate?.let {
            parameters["dimensions"] = Vector2(it.effectiveWidth.toDouble(), it.effectiveHeight.toDouble())

            super.apply(source, arrayOf(it))

            it.copyTo(target[0])
        }
    }
}