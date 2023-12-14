package org.openrndr.extra.jumpfill.ops

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorType
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.jumpflood.*
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.resourceUrl
import org.openrndr.shape.Rectangle

class SDFSmoothUnion : Filter(filterShaderFromCode(jf_sdf_smooth_union, "sdf-smooth-union")) {
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target, clip)
    }
}

class SDFSmoothIntersection : Filter(filterShaderFromCode(jf_sdf_smooth_intersection, "sdf-smooth-intersection")) {
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target, clip)
    }
}
@Description("SDF smooth difference")
class SDFSmoothDifference : Filter(filterShaderFromCode(jf_sdf_smooth_difference, "sdf-smooth-differecnce")) {
    @DoubleParameter("smooth radius", 0.0, 200.0, order = 0)
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target, clip)
    }
}

class SDFRound : Filter(filterShaderFromCode(jf_sdf_round, "sdf-round")) {
    @DoubleParameter("rounding radius", 0.0, 200.0, order = 0)
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target, clip)
    }
}

class SDFOnion : Filter(filterShaderFromCode(jf_sdf_onion, "sdf-onion")) {
    var radius: Double by parameters

    init {
        radius = 0.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target, clip)
    }
}

class SDFBlend : Filter(filterShaderFromCode(jf_sdf_blend, "sdf-blend")) {
    var factor: Double by parameters

    init {
        factor = 0.5
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        super.apply(source, target, clip)
    }
}
