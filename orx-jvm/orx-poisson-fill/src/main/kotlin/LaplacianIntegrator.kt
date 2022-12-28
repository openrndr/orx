package org.openrndr.poissonfill

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.resourceUrl

internal class PassthroughNoAlpha : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/passthrough-noalpha.frag")))

/**
 * Laplacian filter
 */
class Laplacian : Filter1to1(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/laplacian.frag")))

class LaplacianIntegrator(width: Int, height: Int, downscaling: Int = 1, type: ColorType = ColorType.FLOAT32) {
    private val pyramid = ConvolutionPyramid(2 + width / downscaling, 2 + height / downscaling, type = type)
    private val h1 = floatArrayOf(0.15f, 0.5f, 0.7f, 0.5f, 0.15f)
    private val h2 = 1.0f
    private val g = floatArrayOf(0.175f, 0.547f, 0.175f)
    private val preproc = colorBuffer(width + 2, height + 2, type = type)
    private val combined = colorBuffer(width, height)
    private val passthrough = PassthroughNoAlpha()

    init {
        pyramid.h1 = h1
        pyramid.g = g
        pyramid.h2 = h2
    }

    fun process(input: ColorBuffer): ColorBuffer {
        preproc.fill(ColorRGBa.TRANSPARENT)

        pyramid.h1 = h1
        pyramid.g = g
        pyramid.h2 = h2

        passthrough.padding = 1
        passthrough.apply(input, preproc)
        passthrough.padding = 0

        val result = pyramid.process(preproc)
        passthrough.padding = -1
        passthrough.apply(result, combined)
        passthrough.padding = 0
        return combined
    }
}