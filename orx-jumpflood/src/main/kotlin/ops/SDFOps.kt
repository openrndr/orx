package org.openrndr.extra.jumpfill.ops

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorType
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.resourceUrl

class SDFSmoothUnion : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/ops/sdf-smooth-union.frag"))) {
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target)
    }
}

class SDFSmoothIntersection : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/ops/sdf-smooth-intersection.frag"))) {
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target)
    }
}
@Description("SDF smooth difference")
class SDFSmoothDifference : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/ops/sdf-smooth-difference.frag"))) {
    @DoubleParameter("smooth radius", 0.0, 200.0, order = 0)
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target)
    }
}

class SDFRound : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/ops/sdf-round.frag"))) {
    @DoubleParameter("rounding radius", 0.0, 200.0, order = 0)
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target)
    }
}

class SDFOnion : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/ops/sdf-onion.frag"))) {
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target)
    }
}

class SDFBlend : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/ops/sdf-blend.frag"))) {
    var factor: Double by parameters

    init {
        factor = 0.5
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target)
    }

}
