package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

class SubtractConstant : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/subtract-constant.frag"))) {
    var constant: ColorRGBa by parameters

    init {
        constant = ColorRGBa(1.0, 1.0, 1.0, 0.0)
    }
}
