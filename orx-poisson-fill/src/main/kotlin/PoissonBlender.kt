package org.openrndr.poissonfill

import org.openrndr.draw.*
import org.openrndr.filter.blend.subtract
import org.openrndr.resourceUrl

internal class BlendBoundary : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/blend-boundary.frag")))
class AlphaToBitmap : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/alpha-to-bitmap.frag")))

internal class BlendCombine : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/blend-combine.frag"))) {
    var softMaskGain: Double by parameters
    init {
        softMaskGain = 1.0
    }
}

internal class Clamp : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/clamp.frag"))) {
    var minValue: Double by parameters
    var maxValue: Double by parameters
}

class PoissonBlender(width: Int, height: Int, type: ColorType = ColorType.FLOAT32) {
    private val pyramid = ConvolutionPyramid(width, height, 0, type = type)
    private val preprocess = colorBuffer(width, height, type = type)
    private val combined = colorBuffer(width, height, type = type)

    private val fillBoundary = BlendBoundary()
    private val fillCombine = BlendCombine()

    private val difference = colorBuffer(width, height, type = type)

    private val h1 = floatArrayOf(0.1507146f, 0.6835785f, 1.0334191f, 0.6836f, 0.1507f)
    private val h2 = 0.0269546f
    private val g = floatArrayOf(0.0311849f, 0.7752854f, 0.0311849f)

    private val clamp = Clamp()

    init {
        pyramid.h1 = h1
        pyramid.g = g
        pyramid.h2 = h2
    }

    fun process(target: ColorBuffer, source: ColorBuffer, mask: ColorBuffer,
                softMask: ColorBuffer = mask, softMaskGain: Double = 1.0): ColorBuffer {
        subtract.apply(arrayOf(target, source), difference)
        clamp.minValue = -0.50
        clamp.maxValue = 0.50
        fillBoundary.apply(arrayOf(difference, mask), preprocess)
        val result = pyramid.process(preprocess)
        fillCombine.softMaskGain = softMaskGain
        fillCombine.apply(arrayOf(result, target, source, mask, softMask), arrayOf(combined))
        return combined
    }
}