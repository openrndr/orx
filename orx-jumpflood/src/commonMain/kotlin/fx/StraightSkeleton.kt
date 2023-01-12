package org.openrndr.extra.jumpfill.fx

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.*
import org.openrndr.extra.jumpflood.jf_straight_skeleton
import org.openrndr.extra.parameters.ColorParameter

import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import kotlin.math.sqrt

private class StraightSkeletonFilter : Filter(filterShaderFromCode(jf_straight_skeleton, "straight-skeleton")) {
    var angleThreshold: Double by parameters
    var skeletonColor: ColorRGBa by parameters
    var foregroundColor: ColorRGBa by parameters
    var backgroundColor: ColorRGBa by parameters

    init {
        skeletonColor = ColorRGBa.WHITE
        foregroundColor = ColorRGBa.GRAY
        backgroundColor = ColorRGBa.TRANSPARENT
        angleThreshold = sqrt(2.0) / 2.0;
    }
}

@Description("Skeleton")
class StraightSkeleton : Filter() {
    @DoubleParameter("threshold", 0.0, 1.0, order = 0)
    var threshold = 0.5

    @DoubleParameter("distance scale", 0.0, 1.0, order = 1)
    var distanceScale = 1.0

    @DoubleParameter("angle threshold", 0.0, 1.0, order = 2)
    var angleThreshold = sqrt(2.0) / 2.0

    @ColorParameter("skeleton color", order = 3)
    var skeletonColor = ColorRGBa.WHITE

    @ColorParameter("foreground color", order = 4)
    var foregroundColor = ColorRGBa.GRAY

    @ColorParameter("background color", order = 5)
    var backgroundColor = ColorRGBa.TRANSPARENT

    private val thresholdFilter = Threshold()
    private var thresholded: ColorBuffer? = null
    private val contourFilter = ContourPoints()
    private var contoured: ColorBuffer? = null
    private var copied: ColorBuffer? = null
    private var jumpFlooder: JumpFlooder? = null

    private val decodeFilter = PixelDirection()
    private val skeletonFilter = StraightSkeletonFilter()

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
        if (copied == null) {
            copied = target[0].createEquivalent(type = ColorType.FLOAT32)
        }

        thresholdFilter.threshold = threshold
        thresholdFilter.apply(source[0], thresholded!!)
        contourFilter.apply(thresholded!!, contoured!!)
        val result = jumpFlooder!!.jumpFlood(contoured!!)
        decodeFilter.originalSize = Vector2(target[0].width * 1.0, target[0].height * 1.0)
        decodeFilter.distanceScale = distanceScale
        decodeFilter.apply(arrayOf(result, thresholded!!), arrayOf(result))
        result.copyTo(copied!!)

        skeletonFilter.angleThreshold = angleThreshold
        skeletonFilter.skeletonColor = skeletonColor
        skeletonFilter.backgroundColor = backgroundColor
        skeletonFilter.foregroundColor = foregroundColor
        skeletonFilter.apply(copied!!, target[0])
    }
}