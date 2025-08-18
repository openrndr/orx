package org.openrndr.poissonfill

import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough
import org.openrndr.extra.fx.blend.Subtract
import org.openrndr.filter.color.delinearize
import org.openrndr.filter.color.linearize
import org.openrndr.resourceUrl
import org.openrndr.shape.Rectangle

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

private val passthrough by lazy { Passthrough() }
private val subtract by lazy { Subtract() }

class PoissonBlender(val width: Int, val height: Int, type: ColorType = ColorType.FLOAT32) {
    private val pyramid = ConvolutionPyramid(width, height, 0, type = type)
    private val preprocess = colorBuffer(width, height, type = type)
    private val combined = colorBuffer(width, height, type = type)

    private val fillBoundary = BlendBoundary()
    private val fillCombine = BlendCombine()

    private val difference = colorBuffer(width, height, type = type)

    private val h1 = floatArrayOf(0.1507146f, 0.6835785f, 1.0334191f, 0.6836f, 0.1507f)
    private val h2 = 0.0269546f
    private val g = floatArrayOf(0.0311849f, 0.7752854f, 0.0311849f)


    init {
        pyramid.h1 = h1
        pyramid.g = g
        pyramid.h2 = h2
    }

    fun process(target: ColorBuffer, source: ColorBuffer, mask: ColorBuffer,
                softMask: ColorBuffer = mask, softMaskGain: Double = 1.0): ColorBuffer {
        subtract.apply(arrayOf(target, source), difference)
        fillBoundary.apply(arrayOf(difference, mask), preprocess)
        val result = pyramid.process(preprocess)
        fillCombine.softMaskGain = softMaskGain
        fillCombine.apply(arrayOf(result, target, source, mask, softMask), arrayOf(combined))
        return combined
    }

    fun destroy() {
        pyramid.destroy()
        preprocess.destroy()
        combined.destroy()
        difference.destroy()
    }

}

class PoissonBlend: Filter2to1(null) {
    private var blender: PoissonBlender? = null

    val alphaToBitmap = AlphaToBitmap()
    var mask: ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)
        if (target.isNotEmpty()) {

            mask?.let {
                if (it.width != target[0].width || it.height != target[0].height) {
                    it.destroy()
                    mask = null
                }
            }

            if (mask == null) {
                mask = colorBuffer(target[0].width, target[0].height)
            }

            blender?.let {
                if (it.width != target[0].width || it.height != target[0].height) {
                    it.destroy()
                    blender = null
                }
            }

            if (blender == null) {
                blender = PoissonBlender(target[0].width, target[0].height)
            }

            mask?.let {
                alphaToBitmap.apply(source[1], it)
            }


            blender?.let {

                linearize.apply(source[0], source[0])
                linearize.apply(source[1], source[1])

                val result = it.process(source[0], source[1], mask ?: error("no mask"))
                result.copyTo(target[0])

                delinearize.apply(target[0], target[0])
            }

        }
    }
}