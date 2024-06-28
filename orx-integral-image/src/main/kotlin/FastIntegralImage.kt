package org.openrndr.extra.integralimage

import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough

import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.log


internal class FastIntegralImageFilter : Filter(
    filterShaderFromUrl(
        resourceUrl(
            "/shaders/gl3/integral-image.frag"
        )
    )
) {
    var passIndex: Int by parameters
    var passDirection: Vector2 by parameters
    var sampleCount: Int by parameters
    var sampleCountBase: Int by parameters
}

/**
 * Compute an integral image for the source image
 */
class FastIntegralImage : Filter(
    filterShaderFromUrl(
        resourceUrl(
            "/shaders/gl3/integral-image.frag"
        )
    )
) {
    private val passthrough = Passthrough()

    var intermediate: ColorBuffer? = null
    var sourceCropped: ColorBuffer? = null
    var targetPadded: ColorBuffer? = null
    private val filter = FastIntegralImageFilter()

    private fun sampleCounts(size: Int, sampleCountBase: Int): List<Int> {
        var remainder = size
        val sampleCounts = mutableListOf<Int>()
        while (remainder > 0) {
            sampleCounts += if (remainder >= sampleCountBase) {
                sampleCountBase
            } else {
                remainder
            }
            remainder /= sampleCountBase
        }
        return sampleCounts
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)
        require(source[0].isEquivalentTo(target[0], ignoreFormat = true, ignoreType = true))

        val npotx = ceil(log(source[0].effectiveWidth.toDouble(), 2.0)).toInt()
        val npoty = ceil(log(source[0].effectiveHeight.toDouble(), 2.0)).toInt()

        val recWidth = 1 shl npotx
        val recHeight = 1 shl npoty

        if (recWidth != source[0].effectiveWidth || recHeight != source[0].effectiveHeight) {
            if (sourceCropped?.effectiveWidth != recWidth || sourceCropped?.effectiveHeight != recHeight) {
                sourceCropped?.destroy()
                targetPadded?.destroy()
            }

            if (sourceCropped == null) {
                sourceCropped = source[0].createEquivalent(width = recWidth, height = recHeight, contentScale = 1.0)
                targetPadded = target[0].createEquivalent(
                    width = (recWidth / target[0].contentScale).toInt(),
                    height = (recHeight / target[0].contentScale).toInt(),
                    contentScale = 1.0
                )
            }
            source[0].copyTo(sourceCropped!!,
                sourceRectangle = IntRectangle(0, 0, source[0].effectiveWidth, source[0].effectiveHeight),
                targetRectangle = IntRectangle(0, recHeight-source[0].effectiveHeight, source[0].effectiveWidth, source[0].effectiveHeight)
            )
        }

        val sampleCountBase = 16
        val xSampleCounts = sampleCounts(recWidth, sampleCountBase)
        val ySampleCounts = sampleCounts(recHeight, sampleCountBase)

        val li = intermediate
        if (li == null || (li.effectiveWidth != recWidth || li.effectiveHeight != recHeight)) {
            intermediate?.destroy()
            intermediate = colorBuffer(recWidth, recHeight, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        }

        val targets = arrayOf(if (targetPadded == null) target else arrayOf(targetPadded!!), arrayOf(intermediate!!))

        var targetIndex = 0

        filter.sampleCountBase = sampleCountBase

        /*
        Perform horizontal steps
         */
        filter.passDirection = Vector2.UNIT_X
        for (pass in xSampleCounts.indices) {
            filter.sampleCount = xSampleCounts[pass]
            filter.passIndex = pass
            filter.apply(
                if (pass == 0) {
                    if (sourceCropped == null) source else arrayOf(sourceCropped!!)
                } else targets[targetIndex % 2], targets[(targetIndex + 1) % 2]
            )
            targetIndex++
        }


        /*
        Perform vertical steps
         */
        filter.passDirection = Vector2.UNIT_Y
        for (pass in ySampleCounts.indices) {
            filter.sampleCount = ySampleCounts[pass]
            filter.passIndex = pass
            filter.apply(targets[targetIndex % 2], targets[(targetIndex + 1) % 2])
            targetIndex++
        }

        // this is a bit wasteful
        if (targetIndex % 2 == 1) {
            passthrough.apply(targets[1], targets[0])
        }

        /*
        When the source is not a power of two we copy from the padded target to the target
         */
        if (targetPadded != null) {
            targetPadded!!.copyTo(target[0],
                sourceRectangle = IntRectangle(0, recHeight-source[0].effectiveHeight, source[0].effectiveWidth, source[0].effectiveHeight),
                targetRectangle = IntRectangle(0, 0, source[0].effectiveWidth, source[0].effectiveHeight)
            )
        }
    }
}