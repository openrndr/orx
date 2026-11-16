package org.openrndr.poissonfill

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough
import org.openrndr.math.IntVector2
import org.openrndr.resourceUrl
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.min

internal class Downscale(filterUrl: String = "/shaders/gl3/poisson/downscale.frag")
    : Filter(filterShaderFromUrl(resourceUrl(filterUrl))) {
    var h1: FloatArray by parameters
    init {
        h1 = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    }
}

internal class Upscale(filterUrl: String = "/shaders/gl3/poisson/upscale.frag")
    : Filter(filterShaderFromUrl(resourceUrl(filterUrl))) {
    var h1: FloatArray by parameters
    var h2: Float by parameters
    var g: FloatArray by parameters

    init {
        h1 = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        h2 = 0.0f
        g = floatArrayOf(0.0f, 0.0f, 0.0f)
    }
}

/** a pass of the separable filters of the pyramid, with the parameters of all of them */
internal class SeparablePass(filterUrl: String) : Filter(filterShaderFromUrl(resourceUrl(filterUrl))) {
    var h1: FloatArray by parameters
    var h2: Float by parameters
    var g: FloatArray by parameters

    init {
        h1 = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        h2 = 0.0f
        g = floatArrayOf(0.0f, 0.0f, 0.0f)
    }
}

/** levels with more pixels are filtered in a horizontal and a vertical pass, smaller ones in a single pass */
private const val SEPARABLE_PIXELS = 1200 * 1200

internal class Convolution(filterUrl: String = "/shaders/gl3/poisson/filter.frag")
    : Filter(filterShaderFromUrl(resourceUrl(filterUrl))) {
    var g: FloatArray by parameters

    init {
        g = floatArrayOf(0.0f, 0.0f, 0.0f)
    }
}

private val passthrough by lazy { Passthrough() }
internal class ConvolutionPyramid(width: Int, height: Int,
                         private val padding: Int = 0, cutOff: Int = 10000,
                         private val downscale: Downscale = Downscale(),
                         private val upscale: Upscale = Upscale(),
                         private val filter: Convolution = Convolution(),
                         val type: ColorType = ColorType.FLOAT32) {
    var h1 = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    var h2 = 0.0f
    var g = floatArrayOf(0.0f, 0.0f, 0.0f)

    private val size = 5
    private val resolution = IntVector2(width + 2 * padding, height + 2 * padding)
    private val minResolution = min(resolution.x, resolution.y)
    private val depth = min(cutOff, ceil(log2(minResolution.toDouble())).toInt())

    private val levelsIn = mutableListOf<RenderTarget>()
    private val levelsOut = mutableListOf<RenderTarget>()

    private val result = colorBuffer(width, height, type = type)

    private val downscaleH = SeparablePass("/shaders/gl3/poisson/downscale-h.frag")
    private val downscaleV = SeparablePass("/shaders/gl3/poisson/downscale-v.frag")
    private val upscaleFine = SeparablePass("/shaders/gl3/poisson/upscale-h-fine.frag")
    private val upscaleCoarse = SeparablePass("/shaders/gl3/poisson/upscale-h-coarse.frag")
    private val upscaleV = SeparablePass("/shaders/gl3/poisson/upscale-v.frag")

    /** per level that is downscaled from a large level: the level filtered along x only */
    private val downscaled = mutableMapOf<Int, ColorBuffer>()

    /** per large level that is upscaled into: it filtered along x with g, and the level below filtered along x with h1 */
    private val upscaled = mutableMapOf<Int, Pair<ColorBuffer, ColorBuffer>>()

    init {
        var levelWidth = resolution.x + 2 * size
        var levelHeight = resolution.y + 2 * size

        for (i in 0 until depth) {
            levelsIn.add(renderTarget(levelWidth, levelHeight, 1.0) {
                colorBuffer(type = type)
            })

            levelsOut.add(renderTarget(levelWidth, levelHeight, 1.0) {
                colorBuffer(type = type)
            })

            levelWidth /= 2
            levelHeight /= 2
            levelWidth += 2 * size
            levelHeight += 2 * size
        }
        // the input is copied into the middle of the first level, its border stays clear; every other level is
        // written in full by the pass that makes it
        levelsIn[0].clearColor(0, ColorRGBa.TRANSPARENT)

        for (i in levelsIn.indices) {
            if (levelsIn[i].width * levelsIn[i].height <= SEPARABLE_PIXELS) continue
            if (i + 1 < levelsIn.size) {
                downscaled[i + 1] = colorBuffer(levelsIn[i + 1].width, levelsIn[i].height, type = type)
                upscaled[i] = colorBuffer(levelsIn[i].width, levelsIn[i].height, type = type) to
                        colorBuffer(levelsIn[i].width, levelsOut[i + 1].height, type = type)
            }
        }
    }

    fun process(input: ColorBuffer): ColorBuffer {
        downscale.h1 = h1

        upscale.g = g
        upscale.h1 = h1
        upscale.h2 = h2

        filter.g = g

        passthrough.padding = (levelsIn[0].width - input.width) / 2
        passthrough.apply(input, levelsIn[0].colorBuffer(0))
        passthrough.padding = 0

        for (p in listOf(downscaleH, downscaleV, upscaleFine, upscaleCoarse, upscaleV)) {
            p.h1 = h1
            p.h2 = h2
            p.g = g
        }

        for (i in 1 until levelsIn.size) {
            val between = downscaled[i]
            if (between != null) {
                downscaleH.apply(levelsIn[i - 1].colorBuffer(0), between)
                downscaleV.apply(between, levelsIn[i].colorBuffer(0))
            } else {
                downscale.padding = 0
                downscale.apply(levelsIn[i - 1].colorBuffer(0), levelsIn[i].colorBuffer(0))
            }
        }

        filter.apply(levelsIn.last().colorBuffer(0), levelsOut.last().colorBuffer(0))

        for (i in levelsOut.size - 2 downTo 0) {
            val between = upscaled[i]
            if (between != null) {
                val (fine, coarse) = between
                upscaleFine.apply(levelsIn[i].colorBuffer(0), fine)
                upscaleCoarse.apply(levelsOut[i + 1].colorBuffer(0), coarse)
                upscaleV.apply(arrayOf(fine, coarse), arrayOf(levelsOut[i].colorBuffer(0)))
            } else {
                upscale.padding = 0
                upscale.apply(arrayOf(levelsIn[i].colorBuffer(0), levelsOut[i + 1].colorBuffer(0)), arrayOf(levelsOut[i].colorBuffer(0)))
            }
        }

        passthrough.padding = -size - padding
        passthrough.apply(levelsOut[0].colorBuffer(0), result)
        passthrough.padding = 0
        return result
    }

    fun destroy() {
        result.destroy()
        downscaled.values.forEach { it.destroy() }
        upscaled.values.forEach { (a, b) -> a.destroy(); b.destroy() }
        (levelsIn+levelsOut).forEach {
            it.colorAttachments.forEach {
                when(it) {
                    is ColorBufferAttachment -> it.colorBuffer.destroy()
                    is CubemapAttachment -> it.cubemap.destroy()
                    is ArrayTextureAttachment -> it.arrayTexture.destroy()
                    is ArrayCubemapAttachment -> it.arrayCubemap.destroy()
                    else -> {}
                }
            }
            it.detachColorAttachments()
            it.destroy()
        }
    }
}