package org.openrndr.extra.fx.shadow


import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector2

private class Blend : Filter(filterShaderFromUrl(filterFragmentUrl("shadow/dropshadow-blend.frag"))) {
    var shift: Vector2 by parameters
}

@Description("Drop shadow")
class DropShadow : Filter(filterShaderFromUrl(filterFragmentUrl("shadow/dropshadow-blur.frag"))) {

    @IntParameter("blur window", 1, 25)
    var window: Int by parameters
    var spread: Double by parameters
    @DoubleParameter("gain", 0.0, 4.0)
    var gain: Double by parameters

    @DoubleParameter("x shift", -30.0, 30.0)
    var xShift: Double = 0.0

    @DoubleParameter("y shift", -30.0, 30.0)
    var yShift: Double = 0.0

    @ColorParameter("color")
    var color: ColorRGBa by parameters

    private var intermediate: ColorBuffer? = null
    private var intermediate2: ColorBuffer? = null
    private var b = Blend()

    init {
        color = ColorRGBa.BLACK
        window = 5
        spread = 1.0
        gain = 1.0
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        intermediate?.let {
            if (it.width != target[0].width || it.height != target[0].height) {
                intermediate = null
            }
        }
        intermediate2?.let {
            if (it.width != target[0].width || it.height != target[0].height) {
                intermediate2 = null
            }
        }
        if (intermediate == null) {
            intermediate = colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }
        if (intermediate2 == null) {
            intermediate2 = colorBuffer(target[0].width, target[0].height, target[0].contentScale, target[0].format, target[0].type)
        }

        intermediate?.let {
            parameters["blurDirection"] = Vector2(1.0, 0.0)
            super.apply(source, arrayOf(it))

            parameters["blurDirection"] = Vector2(0.0, 1.0)
            super.apply(arrayOf(it), arrayOf(intermediate2!!))

            b.shift = (Vector2(xShift,yShift)) / Vector2(target[0].width * 1.0, target[0].height * 1.0)
            b.apply(arrayOf(intermediate2!!, source[0]), target)
        }
    }
}