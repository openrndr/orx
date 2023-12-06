@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blend

import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.Filter2to1
import org.openrndr.extra.fx.*
import org.openrndr.extra.parameters.BooleanParameter

class ColorBurn : Filter2to1(mppFilterShader(fx_color_burn, "color-burn")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class ColorDodge : Filter2to1(mppFilterShader(fx_color_dodge, "color-dodge")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Darken : Filter2to1(mppFilterShader(fx_darken, "darken")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class HardLight : Filter2to1(mppFilterShader(fx_hard_light, "hard-light")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Lighten : Filter2to1(mppFilterShader(fx_lighten, "lighten")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Multiply : Filter2to1(mppFilterShader(fx_multiply,"multiply")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Normal : Filter2to1(mppFilterShader(fx_normal, "normal")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Overlay : Filter2to1(mppFilterShader(fx_overlay, "overlay")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Screen : Filter2to1(mppFilterShader(fx_screen, "screen")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}


class SourceIn : Filter2to1(mppFilterShader(fx_source_in, "source-in"))
class SourceOut : Filter2to1(mppFilterShader(fx_source_out,"source-out"))
class SourceAtop : Filter2to1(mppFilterShader(fx_source_atop, "source-atop"))
class DestinationIn : Filter2to1(mppFilterShader(fx_destination_in, "destination-in"))
class DestinationOut : Filter2to1(mppFilterShader(fx_destination_out, "destination-out"))
class DestinationAtop : Filter2to1(mppFilterShader(fx_destination_atop, "destination-atop"))
class Xor : Filter2to1(mppFilterShader(fx_xor, "xor"))

class MultiplyContrast : Filter2to1(mppFilterShader(fx_multiply_contrast, "multiply-contrast"))

class Passthrough : Filter1to1(mppFilterShader(fx_passthrough, "passthrough"))
class Add : Filter2to1(mppFilterShader(fx_add, "add")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}
class Subtract : Filter2to1(mppFilterShader(fx_subtract,"subtract")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}