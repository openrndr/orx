package org.openrndr.extra.jumpfill.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.jumpflood.jf_sdf_stroke_fill
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("SDF stroke and fill")
class SDFStrokeFill : Filter(filterShaderFromCode(jf_sdf_stroke_fill, "sdf-stroke-fill")) {
    @DoubleParameter("stroke weight", 0.0, 20.0, order = 0)
    var strokeWeight: Double by parameters

    @DoubleParameter("stroke feather", 0.0, 20.0, order = 0)
    var strokeFeather: Double by parameters

    @ColorParameter("stroke color", order = 1)
    var strokeColor: ColorRGBa by parameters

    @DoubleParameter("fill feather", 0.0, 20.0, order = 0)
    var fillFeather: Double by parameters


    @ColorParameter("fill color", order = 2)
    var fillColor: ColorRGBa by parameters
    init {
        fillFeather = 1.0
        strokeFeather = 1.0
        strokeWeight = 1.0
        strokeColor = ColorRGBa.BLACK
        fillColor = ColorRGBa.WHITE

    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        super.apply(source, target)
    }
}
