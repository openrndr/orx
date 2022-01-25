package org.openrndr.extra.jumpfill

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.Filter
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

@Description("Distance field")
class DistanceField : Filter() {
    @DoubleParameter("threshold", 0.0, 1.0)
    var threshold = 0.5

    @DoubleParameter("distance scale", 0.0, 1.0)
    var distanceScale = 1.0

    private val thresholdFilter = Threshold()
    private var thresholded: ColorBuffer? = null
    private val contourFilter = ContourPoints()
    private var contoured: ColorBuffer? = null
    private var jumpFlooder: JumpFlooder? = null

    private val decodeFilter = PixelDistance()

    var signedDistance = true

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (thresholded == null) {
            thresholded = colorBuffer(target[0].width, target[0].height, format = ColorFormat.R)
        }
        if (contoured == null) {
            contoured = colorBuffer(target[0].width, target[0].height, format = ColorFormat.R)
        }
        if (jumpFlooder == null) {
            jumpFlooder = JumpFlooder(target[0].width, target[0].height)
        }
        thresholdFilter.threshold = threshold
        thresholdFilter.apply(source[0], thresholded!!)
        contourFilter.apply(thresholded!!, contoured!!)
        val result = jumpFlooder!!.jumpFlood(contoured!!)
        decodeFilter.signedDistance = signedDistance
        decodeFilter.originalSize = Vector2(target[0].width * 1.0, target[0].height * 1.0)
        decodeFilter.distanceScale = distanceScale
        decodeFilter.signedBit = false
        decodeFilter.apply(arrayOf(result, thresholded!!), arrayOf(result))
        result.copyTo(target[0])
    }
}