package org.openrndr.extra.fx.edges

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_canny_edge_detector
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Canny Edge Detector")
class CannyEdgeDetector : Filter(
    filterShaderFromCode(fx_canny_edge_detector, "canny-edge-detector")
) {

    @DoubleParameter("threshold 0", 0.0, 100.0, order = 0)
    var threshold0: Double by parameters

    @DoubleParameter("threshold 1", 0.0, 100.0, order = 1)
    var threshold1: Double by parameters

    @DoubleParameter("thickness", 0.0, 10.0, order = 2)
    var thickness: Double by parameters

    @ColorParameter("foreground color", order = 3)
    var foregroundColor: ColorRGBa by parameters

    @DoubleParameter("foreground opacity", 0.0, 1.0, order = 4)
    var foregroundOpacity: Double by parameters

    @ColorParameter("background color", order = 5)
    var backgroundColor: ColorRGBa by parameters

    @DoubleParameter("background opacity", 0.0, 1.0, order = 6)
    var backgroundOpacity: Double by parameters


    init {
        threshold0 = 2.0
        threshold1 = 0.0
        thickness = 1.0
        foregroundColor = ColorRGBa.WHITE
        backgroundColor = ColorRGBa.BLACK
        backgroundOpacity = 1.0
        foregroundOpacity = 1.0
    }

}
