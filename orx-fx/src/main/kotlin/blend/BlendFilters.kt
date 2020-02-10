package org.openrndr.extra.fx.blend

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.BooleanParameter

class ColorBurn : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/color-burn.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class ColorDodge : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/color-dodge.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Darken : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/darken.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class HardLight : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/hard-light.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Lighten : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/lighten.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Multiply : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/multiply.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Normal : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/normal.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Overlay : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/overlay.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}

class Screen : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/screen.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}


class SourceIn : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/source-in.frag")))
class SourceOut : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/source-out.frag")))
class DestinationIn : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/destination-in.frag")))
class DestinationOut : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/destination-out.frag")))
class Xor : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/xor.frag")))

class MultiplyContrast : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/multiply-contrast.frag")))

class Passthrough : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/passthrough.frag")))
class Add : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/add.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}
class Subtract : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("blend/subtract.frag"))) {
    @BooleanParameter("source clip")
    var clip: Boolean by parameters

    init {
        clip = false
    }
}
