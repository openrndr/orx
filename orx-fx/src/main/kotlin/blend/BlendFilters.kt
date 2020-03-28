package org.openrndr.extra.fx.blend

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.BooleanParameter

class ColorBurn : Filter(filterShaderFromUrl(filterFragmentUrl("blend/color-burn.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class ColorDodge : Filter(filterShaderFromUrl(filterFragmentUrl("blend/color-dodge.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Darken : Filter(filterShaderFromUrl(filterFragmentUrl("blend/darken.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class HardLight : Filter(filterShaderFromUrl(filterFragmentUrl("blend/hard-light.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Lighten : Filter(filterShaderFromUrl(filterFragmentUrl("blend/lighten.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Multiply : Filter(filterShaderFromUrl(filterFragmentUrl("blend/multiply.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Normal : Filter(filterShaderFromUrl(filterFragmentUrl("blend/normal.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Overlay : Filter(filterShaderFromUrl(filterFragmentUrl("blend/overlay.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Screen : Filter(filterShaderFromUrl(filterFragmentUrl("blend/screen.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}


class SourceIn : Filter(filterShaderFromUrl(filterFragmentUrl("blend/source-in.frag")))
class SourceOut : Filter(filterShaderFromUrl(filterFragmentUrl("blend/source-out.frag")))
class SourceAtop : Filter(filterShaderFromUrl(filterFragmentUrl("blend/source-atop.frag")))
class DestinationIn : Filter(filterShaderFromUrl(filterFragmentUrl("blend/destination-in.frag")))
class DestinationOut : Filter(filterShaderFromUrl(filterFragmentUrl("blend/destination-out.frag")))
class DestinationAtop : Filter(filterShaderFromUrl(filterFragmentUrl("blend/destination-atop.frag")))
class Xor : Filter(filterShaderFromUrl(filterFragmentUrl("blend/xor.frag")))

class MultiplyContrast : Filter(filterShaderFromUrl(filterFragmentUrl("blend/multiply-contrast.frag")))

class Passthrough : Filter(filterShaderFromUrl(filterFragmentUrl("blend/passthrough.frag")))
class Add : Filter(filterShaderFromUrl(filterFragmentUrl("blend/add.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}
class Subtract : Filter(filterShaderFromUrl(filterFragmentUrl("blend/subtract.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}
