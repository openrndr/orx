package org.openrndr.extra.fx.color

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_chromatic_aberration
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

@Description("Chromatic Aberration")
class ChromaticAberration : Filter(mppFilterShader(fx_chromatic_aberration, "chromatic-aberration")) {
    /**
     * aberration factor, default value is 8.0
     */
    @DoubleParameter("aberration factor", 0.0, 16.0)
    var aberrationFactor: Double by parameters

    init {
        aberrationFactor = 8.0
    }

    private var intermediate: ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        intermediate?.let {
            if (it.width != target[0].width || it.height != target[0].height) {
                intermediate = null
            }
        }

        if (intermediate == null) {
            intermediate = colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }

        intermediate?.let {
            parameters["dimensions"] = Vector2(it.effectiveWidth.toDouble(), it.effectiveHeight.toDouble())

            super.apply(source, arrayOf(it))

            it.copyTo(target[0])
        }
    }
}
