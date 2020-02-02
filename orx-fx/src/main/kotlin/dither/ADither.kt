package org.openrndr.extra.fx.dither

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter

@Description("ADither")
class ADither: Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("dither/a-dither.frag"))) {
    @IntParameter("pattern index", 0, 3)
    var pattern: Int by parameters

    @IntParameter("levels", 1, 64)
    var levels: Int by parameters

    init {
        levels = 4
        pattern = 3
    }
}