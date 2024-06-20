@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_posterize
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter

@Description("Posterize")
class Posterize : Filter1to1(filterShaderFromCode(fx_posterize, "posterize")) {

    @IntParameter("levels", 2, 32, order = 0)
    var levels: Int by parameters

    @IntParameter("window", 1, 8, order = 0)
    var window: Int by parameters

    init {
        levels = 4
        window = 1
    }
}