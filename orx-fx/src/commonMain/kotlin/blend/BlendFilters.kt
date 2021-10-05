package org.openrndr.extra.fx.blend

import org.openrndr.draw.Filter
import org.openrndr.extra.fx.*
import org.openrndr.extra.parameters.BooleanParameter

class ColorBurn : Filter(mppFilterShader(fx_color_burn, "color-burn")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class ColorDodge : Filter(mppFilterShader(fx_color_dodge, "color-dodge")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Darken : Filter(mppFilterShader(fx_darken, "darken")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class HardLight : Filter(mppFilterShader(fx_hard_light, "hard-light")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Lighten : Filter(mppFilterShader(fx_lighten, "lighten")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Multiply : Filter(mppFilterShader(fx_multiply,"multiply")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Normal : Filter(mppFilterShader(fx_normal, "normal")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Overlay : Filter(mppFilterShader(fx_overlay, "overlay")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Screen : Filter(mppFilterShader(fx_screen, "screen")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}


class SourceIn : Filter(mppFilterShader(fx_source_in, "source-in"))
class SourceOut : Filter(mppFilterShader(fx_source_out,"source-out"))
class SourceAtop : Filter(mppFilterShader(fx_source_atop, "source-atop"))
class DestinationIn : Filter(mppFilterShader(fx_destination_in, "destination-in"))
class DestinationOut : Filter(mppFilterShader(fx_destination_out, "destination-out"))
class DestinationAtop : Filter(mppFilterShader(fx_destination_atop, "destination-atop"))
class Xor : Filter(mppFilterShader(fx_xor, "xor"))

class MultiplyContrast : Filter(mppFilterShader(fx_multiply_contrast, "multiply-contrast"))

class Passthrough : Filter(mppFilterShader(fx_passthrough, "passthrough"))
class Add : Filter(mppFilterShader(fx_add, "add")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}
class Subtract : Filter(mppFilterShader(fx_subtract,"subtract")) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}