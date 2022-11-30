package org.openrndr.poissonfill

import org.openrndr.draw.*
import org.openrndr.resourceUrl

internal class FillBoundary : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/fill-boundary.frag")))
internal class FillCombine : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/poisson/fill-combine.frag")))

class PoissonFiller(val width: Int, val height: Int, type: ColorType = ColorType.FLOAT32) {
    private val pyramid = ConvolutionPyramid(width, height, 0, type = type)
    private val preproc = colorBuffer(width, height, type = type)
    private val combined = colorBuffer(width, height, type = type)
    private val fillBoundary = FillBoundary()
    private val fillCombine = FillCombine()

    private val h1 = floatArrayOf(0.1507146f, 0.6835785f, 1.0334191f, 0.6836f, 0.1507f)
    private val h2 = 0.0269546f
    private val g = floatArrayOf(0.0311849f, 0.7752854f, 0.0311849f)

    init {
        pyramid.h1 = h1
        pyramid.g = g
        pyramid.h2 = h2
    }

    fun process(input: ColorBuffer): ColorBuffer {
        fillBoundary.apply(input, preproc)
        val result = pyramid.process(preproc)
        fillCombine.apply(arrayOf(result, input), arrayOf(combined))
        return combined
    }

    fun destroy() {
        preproc.destroy()
        combined.destroy()
    }
}

/**
 * Poison filling as a filter
 */
class PoissonFill : Filter() {
    private var filler: PoissonFiller? = null
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (target.isNotEmpty()) {
            filler?.let {
                if (it.width != target[0].effectiveWidth || it.height != target[0].effectiveHeight) {
                    it.destroy()
                    filler = null
                }
            }

            if (filler == null) {
                filler = PoissonFiller(target[0].effectiveWidth, target[0].effectiveHeight)
            }

            filler?.let {
                val result = it.process(source[0])
                result.copyTo(target[0])
            }
        }
    }
}