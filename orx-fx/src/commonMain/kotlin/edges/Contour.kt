package org.openrndr.extra.fx.edges

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_contour
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Contour")
class Contour : Filter(mppFilterShader(fx_contour, "contour")) {
    @DoubleParameter("levels", 1.0, 16.0)
    var levels: Double by parameters

    @DoubleParameter("contour width", 0.0, 4.0)
    var contourWidth: Double by parameters

    @DoubleParameter("contour opacity", 0.0, 1.0)
    var contourOpacity: Double by parameters

    @DoubleParameter("background opacity", 0.0, 1.0)
    var backgroundOpacity: Double by parameters

    @ColorParameter("contour color")
    var contourColor: ColorRGBa by parameters

    init {
        levels = 6.0
        contourWidth = 0.4
        contourColor = ColorRGBa.BLACK
        backgroundOpacity = 1.0
        contourOpacity = 1.0
    }
}
