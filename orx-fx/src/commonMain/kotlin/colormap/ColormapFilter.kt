@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.colormap

import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.DoubleParameter

abstract class ColormapFilter(code: String, name: String) : Filter1to1(mppFilterShader(code, name)) {

    @DoubleParameter(label = "min value", low = 0.0, high = 1.0, order = 0)
    var minValue: Double by parameters

    @DoubleParameter(label = "max value", low = 0.0, high = 1.0, order = 1)
    var maxValue: Double by parameters

    @DoubleParameter(label = "curve", low = 0.001, high = 10.0, order = 2)
    var curve: Double by parameters

    init {
        minValue = 0.0
        maxValue = 1.0
        curve = 1.0
    }

}
