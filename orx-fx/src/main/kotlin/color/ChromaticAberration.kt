package org.openrndr.extra.fx.color

import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl

class ChromaticAberration : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/chromatic-aberration.frag"))){
    /**
     * aberration factor, default value is 1.0
     */
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
