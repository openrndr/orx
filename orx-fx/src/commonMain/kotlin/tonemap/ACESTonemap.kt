@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.tonemap

import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_aces_tonemap
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("ACES tonemap")
class ACESTonemap : Filter1to1(mppFilterShader(fx_aces_tonemap, "aces-tonemap")) {
    @DoubleParameter("exposure bias", 0.0, 128.0)
    var exposureBias:Double by parameters
    init {
        exposureBias = 1.0
    }
}