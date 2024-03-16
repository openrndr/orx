@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.jumpfill

import org.openrndr.draw.*
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

@Description("Directional field")
class DirectionalField : Filter1to1() {
    @DoubleParameter("threshold", 0.0, 1.0)
    var threshold = 0.5

    @DoubleParameter("distance scale", 0.0, 1.0)
    var distanceScale = 1.0

    @BooleanParameter("normalized distance")
    var normalizedDistance = false

    @BooleanParameter("unit direction")
    var unitDirection = false

    @BooleanParameter("flip v direction")
    var flipV = true

    private val thresholdFilter = Threshold()
    private var thresholded: ColorBuffer? = null
    private val contourFilter = ContourPoints()
    private var contoured: ColorBuffer? = null
    private var jumpFlooder: JumpFlooder? = null

    private val decodeFilter = PixelDirection()

    private var fit: ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)

        val advisedWidth = 2.0.pow(ceil(log2(source[0].effectiveWidth.toDouble()))).toInt()
        val advisedHeight = 2.0.pow(ceil(log2(source[0].effectiveHeight.toDouble()))).toInt()
        val advisedSize = max(advisedWidth, advisedHeight)

        fit?.let {
            if (it.effectiveWidth != advisedSize || it.effectiveHeight != advisedSize) {
                it.destroy()
                fit = null
                thresholded?.destroy()
                thresholded = null
                contoured?.destroy()
                contoured = null
                jumpFlooder?.destroy()
                jumpFlooder = null
            }
        }

        if (fit == null) {
            fit = colorBuffer(advisedSize, advisedSize)
        }

        source[0].copyTo(fit!!,
            sourceRectangle = IntRectangle(0, 0, source[0].effectiveWidth, source[0].effectiveHeight),
            targetRectangle = IntRectangle(0, advisedSize-source[0].effectiveHeight, source[0].effectiveWidth, source[0].effectiveHeight)
            )

        if (thresholded == null) {
            thresholded = colorBuffer(advisedSize, advisedSize, format = ColorFormat.R)
        }
        if (contoured == null) {
            contoured = colorBuffer(advisedSize, advisedSize, format = ColorFormat.R)
        }
        if (jumpFlooder == null) {
            jumpFlooder = JumpFlooder(advisedSize, advisedSize)
        }
        thresholdFilter.threshold = threshold
        thresholdFilter.apply(fit!!, thresholded!!)
        contourFilter.apply(thresholded!!, contoured!!)
        val result = jumpFlooder!!.jumpFlood(contoured!!)
        decodeFilter.originalSize = Vector2(source[0].width.toDouble(), source[0].height.toDouble())
        decodeFilter.distanceScale = distanceScale
        decodeFilter.normalizedDistance = normalizedDistance
        decodeFilter.unitDirection = unitDirection
        decodeFilter.flipV = flipV
        decodeFilter.apply(arrayOf(result, thresholded!!), arrayOf(result))
        result.copyTo(target[0],
            sourceRectangle = IntRectangle(0, advisedSize-source[0].effectiveHeight, source[0].effectiveWidth, source[0].effectiveHeight),
            targetRectangle = IntRectangle(0, 0, source[0].effectiveWidth, source[0].effectiveHeight))
    }

    override fun destroy() {
        thresholdFilter.destroy()
        contourFilter.destroy()
        fit?.destroy()
        thresholded?.destroy()
        contoured?.destroy()
        jumpFlooder?.destroy()
    }
}