package org.openrndr.extra.fx.distort

import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_video_glitch
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Video glitch")
class VideoGlitch : Filter1to1(mppFilterShader(fx_video_glitch, "video-glitch")) {
    var time: Double by parameters

    @DoubleParameter("amplitude", 0.0, 10.0)
    var amplitude: Double by parameters

    @DoubleParameter("border height", 0.0, 0.5)
    var borderHeight: Double by parameters

    @DoubleParameter("vertical frequency", 0.0, 10.0)
    var vfreq: Double by parameters

    @DoubleParameter("horizontal frequency", 0.0, 80.0)
    var hfreq: Double by parameters

    @DoubleParameter("p frequency", 0.0, 10.0)
    var pfreq: Double by parameters

    @DoubleParameter("p offset", -1.0, 1.0)
    var poffset: Double by parameters

    @DoubleParameter("scroll offset 0", 0.0, 1.0)
    var scrollOffset0: Double by parameters

    @DoubleParameter("scroll offset 1", 0.0, 1.0)
    var scrollOffset1: Double by parameters

    @BooleanParameter("linear input")
    var linearInput: Boolean by parameters

    @BooleanParameter("linear output")
    var linearOutput: Boolean by parameters

    init {
        amplitude = 1.0
        vfreq = 4.0
        pfreq = 10.0
        hfreq = 80.0
        poffset = 0.0
        scrollOffset0 = 0.0
        scrollOffset1 = 0.0
        borderHeight = 0.05
        linearInput = false
        linearOutput = false
    }
}