package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl

class SubtractConstant : Filter(filterShaderFromUrl(filterFragmentUrl("color/subtract-constant.frag"))) {
    var constant: ColorRGBa by parameters

    init {
        constant = ColorRGBa(1.0, 1.0, 1.0, 0.0)
    }
}
