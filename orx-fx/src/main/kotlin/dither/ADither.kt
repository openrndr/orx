package org.openrndr.extra.fx.dither

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter

@Description("ADither")
class ADither: Filter(filterShaderFromUrl(filterFragmentUrl("dither/a-dither.frag"))) {
    @IntParameter("pattern index", 0, 3)
    var pattern: Int by parameters

    @IntParameter("levels", 1, 64)
    var levels: Int by parameters

    init {
        levels = 4
        pattern = 3
    }
}