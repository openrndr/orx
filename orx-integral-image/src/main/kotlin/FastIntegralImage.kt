package org.openrndr.extra.integralimage

import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough

import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import org.openrndr.shape.Rectangle


class FastIntegralImageFilter : Filter(filterShaderFromUrl(resourceUrl(
        "/shaders/gl3/integral-image.frag"
))) {
    var passIndex: Int by parameters
    var passDirection: Vector2 by parameters
    var sampleCount: Int by parameters
    var sampleCountBase: Int by parameters
}

class FastIntegralImage : Filter(filterShaderFromUrl(resourceUrl(
        "/shaders/gl3/integral-image.frag"
))) {
    private val passthrough = Passthrough()

    var intermediate: ColorBuffer? = null
    val filter = FastIntegralImageFilter()

    private fun sampleCounts(size:Int, sampleCountBase:Int) : List<Int> {
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
        val sampleCountBase = 16
        val xSampleCounts = sampleCounts(source[0].width, sampleCountBase)
        val ySampleCounts = sampleCounts(source[0].height, sampleCountBase)

        val li = intermediate
        if (li == null || (li.width != source[0].width || li.height != source[0].height)) {
            intermediate?.destroy()
            intermediate = colorBuffer(source[0].width, source[0].height, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        }

        val targets = arrayOf(target, arrayOf(intermediate!!))

        var targetIndex = 0

        filter.sampleCountBase = sampleCountBase

        filter.passDirection = Vector2.UNIT_X
        for (pass in xSampleCounts.indices) {
            filter.sampleCount = xSampleCounts[pass]
            filter.passIndex = pass
            filter.apply( if (pass == 0) source else targets[targetIndex%2], targets[(targetIndex+1)%2])
            targetIndex++
        }

        filter.passDirection = Vector2.UNIT_Y
        for (pass in ySampleCounts.indices) {
            filter.sampleCount = ySampleCounts[pass]
            filter.passIndex = pass
            filter.apply( targets[targetIndex%2], targets[(targetIndex+1)%2])
            targetIndex++
        }

        if (targetIndex%2 == 1) {
            passthrough.apply(targets[1], targets[0])
        }
    }
}