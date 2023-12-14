package org.openrndr.extra.jumpfill.fx

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.*
import org.openrndr.extra.jumpflood.jf_skeleton
import org.openrndr.extra.parameters.ColorParameter

import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import org.openrndr.shape.Rectangle

private class SkeletonFilter : Filter(filterShaderFromCode(jf_skeleton, "skeleton")) {
    var skeletonColor: ColorRGBa by parameters
    var foregroundColor: ColorRGBa by parameters
    var backgroundColor: ColorRGBa by parameters

    init {
        skeletonColor = ColorRGBa.WHITE
        foregroundColor = ColorRGBa.GRAY
        backgroundColor = ColorRGBa.TRANSPARENT
    }
}

@Description("Skeleton")
class Skeleton : Filter() {
    @DoubleParameter("threshold", 0.0, 1.0, order = 0)
    var threshold = 0.5

    @DoubleParameter("distance scale", 0.0, 1.0, order = 1)
    var distanceScale = 1.0

    @ColorParameter("skeleton color", order = 2)
    var skeletonColor = ColorRGBa.WHITE

    @ColorParameter("foreground color", order = 3)
    var foregroundColor = ColorRGBa.GRAY

    @ColorParameter("background color", order = 4)
    var backgroundColor = ColorRGBa.TRANSPARENT

    private val thresholdFilter = Threshold()
    private var thresholded: ColorBuffer? = null
    private val contourFilter = ContourPoints()
    private var contoured: ColorBuffer? = null
    private var copied: ColorBuffer? = null
    private var jumpFlooder: JumpFlooder? = null

    private val decodeFilter = PixelDistance()
    private val skeletonFilter = SkeletonFilter()

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)

        if (thresholded == null) {
            thresholded = colorBuffer(target[0].width, target[0].height, format = ColorFormat.R)
        }
        if (contoured == null) {
            contoured = colorBuffer(target[0].width, target[0].height, format = ColorFormat.R)
        }
        if (jumpFlooder == null) {
            jumpFlooder = JumpFlooder(target[0].width, target[0].height)
        }
        if (copied == null) {
            copied = target[0].createEquivalent(type = ColorType.FLOAT32)
        }

        thresholdFilter.threshold = threshold
        thresholdFilter.apply(source[0], thresholded!!)
        contourFilter.apply(thresholded!!, contoured!!)
        val result = jumpFlooder!!.jumpFlood(contoured!!)

        decodeFilter.signedDistance = true
        decodeFilter.originalSize = Vector2(target[0].width * 1.0, target[0].height * 1.0)
        decodeFilter.distanceScale = distanceScale
        decodeFilter.signedBit = false
        decodeFilter.apply(arrayOf(result, thresholded!!), arrayOf(result))

        result.copyTo(copied!!)
        skeletonFilter.skeletonColor = skeletonColor
        skeletonFilter.backgroundColor = backgroundColor
        skeletonFilter.foregroundColor = foregroundColor
        skeletonFilter.apply(copied!!, target[0])
    }
}