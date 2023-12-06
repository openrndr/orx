@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.tonemap

import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_reinhard_tonemap
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("ACES tonemap")
class ReinhardTonemap : Filter1to1(mppFilterShader(fx_reinhard_tonemap, "reinhard-tonemap")) {
    @DoubleParameter("exposure bias", 0.0, 128.0)
    var exposureBias:Double by parameters

    @DoubleParameter("maximum luminance", 0.0, 128.0)
    var maxLuminance:Double by parameters
    init {
        exposureBias = 1.0
        maxLuminance = 1.0
    }
}