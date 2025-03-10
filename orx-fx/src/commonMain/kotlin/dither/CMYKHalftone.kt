@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.dither

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_cmyk_halftone
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.shaderphrases.sdf.sdCirclePhrase

@Description("CMYK Halftone")
class CMYKHalftone(
    domainWarpFunction: String = "vec2 domainWarp(vec2 p) { return p; }",
    elementFunction: String = """
    $sdCirclePhrase
    float element(in vec2 p, float v) {
        return sdCircle(p, v * dotSize);
    }""".trimIndent()
) : Filter1to1(
    filterShaderFromCode(
        fx_cmyk_halftone.split("#pragma INSERT_PHRASES").let {
            listOf(it[0], elementFunction, domainWarpFunction, it[1])
        }.joinToString("\n"),
        "cmyk-halftone"
    )
) {
    @DoubleParameter("scale", 1.0, 30.0, precision = 4)
    var scale: Double by parameters

    @DoubleParameter("dotSize", 1.0, 3.0, precision = 4)
    var dotSize: Double by parameters

    @DoubleParameter("rotation", -180.0, 180.0)
    var rotation: Double by parameters

    @DoubleParameter("cyan rotation", -180.0, 180.0, precision = 4)
    var cyanRotation: Double by parameters

    @DoubleParameter("magenta rotation", -180.0, 180.0, precision = 4)
    var magentaRotation: Double by parameters

    @DoubleParameter("yellow rotation", -180.0, 180.0, precision = 4)
    var yellowRotation: Double by parameters

    @DoubleParameter("black rotation", -180.0, 180.0, precision = 4)
    var blackRotation: Double by parameters

    @ColorParameter("cyan color")
    var cyanColor: ColorRGBa by parameters

    @ColorParameter("magenta color")
    var magentaColor: ColorRGBa by parameters

    @ColorParameter("yellow color")
    var yellowColor: ColorRGBa by parameters

    @ColorParameter("black color")
    var blackColor: ColorRGBa by parameters

    var phase: Double by parameters

    init {
        blackRotation = 45.0
        magentaRotation = 75.0
        cyanRotation = 15.0
        yellowRotation = 0.0
        cyanColor = ColorRGBa.CYAN
        magentaColor = ColorRGBa.MAGENTA
        yellowColor = ColorRGBa.YELLOW
        blackColor = ColorRGBa.BLACK

        scale = 3.0
        rotation = 0.0
        dotSize = 0.9
        phase = 0.0
    }
}