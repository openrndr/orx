@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_block_repeat
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.shape.Rectangle

@Description("Block repeat")
class BlockRepeat : Filter1to1(mppFilterShader(fx_block_repeat, "block-repeat")) {
    @DoubleParameter("block width", 0.0, 1.0, order = 0)
    var blockWidth: Double by parameters

    @DoubleParameter("block height", 0.0, 1.0, order = 1)
    var blockHeight: Double by parameters

    @DoubleParameter("block x-offset", -0.5, 0.5, order = 2)
    var blockOffsetX: Double by parameters

    @DoubleParameter("block y-offset", -0.5, 0.5, order = 3)
    var blockOffsetY: Double by parameters

    /**
     * Source scale, 0.0 is a 1:1 mapping, 1.0 fits entire source image in block
     */
    @DoubleParameter("source scale", 0.0, 1.0, order = 4)
    var sourceScale: Double by parameters

    @DoubleParameter("source x-offset", -0.5, 0.5, order = 5)
    var sourceOffsetX: Double by parameters

    @DoubleParameter("source y-offset", -.5, .5, order = 6)
    var sourceOffsetY: Double by parameters

    @BooleanParameter("bicubic filtering")
    var bicubicFiltering: Boolean by parameters

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target, clip)
    }

    init {
        blockWidth = 0.25
        blockHeight = 0.25
        blockOffsetX = 0.0
        blockOffsetY = 0.0
        sourceOffsetX = 0.0
        sourceOffsetY = 0.0
        sourceScale = 0.0
        bicubicFiltering = true
    }
}
