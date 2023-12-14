@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_rectangular_to_polar
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.log

@Description("Rectangular to polar")
class RectangularToPolar : Filter1to1(mppFilterShader(fx_rectangular_to_polar, "rectangular-to-polar")) {

    @BooleanParameter("log polar")
    var logPolar:Boolean by parameters

    init {
        logPolar = true
    }


    var bicubicFiltering = true
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target, clip)
    }
}