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

    init {
        var levelWidth = resolution.x + 2 * size
        var levelHeight = resolution.y + 2 * size

        for (i in 0 until depth) {
            levelsIn.add(renderTarget(levelWidth, levelHeight) {
                colorBuffer(type = type)
            })

            levelsOut.add(renderTarget(levelWidth, levelHeight) {
                colorBuffer(type = type)
            })

            levelWidth /= 2
            levelHeight /= 2
            levelWidth += 2 * size
            levelHeight += 2 * size
        }
    }

    fun process(input: ColorBuffer): ColorBuffer {
        for (l in levelsIn) {
            l.clearColor(0, ColorRGBa.TRANSPARENT)
        }

        for (l in levelsOut) {
            l.clearColor(0, ColorRGBa.TRANSPARENT)
        }

        downscale.h1 = h1

        upscale.g = g
        upscale.h1 = h1
        upscale.h2 = h2

        filter.g = g

        passthrough.padding = (levelsIn[0].width - input.width) / 2
        passthrough.apply(input, levelsIn[0].colorBuffer(0))
        passthrough.padding = 0

        for (i in 1 until levelsIn.size) {
            downscale.padding = 0
            downscale.apply(levelsIn[i - 1].colorBuffer(0),
                    levelsIn[i].colorBuffer(0)
            )
        }

        filter.apply(levelsIn.last().colorBuffer(0), levelsOut.last().colorBuffer(0))

        for (i in levelsOut.size - 2 downTo 0) {
            upscale.padding = 0
            upscale.apply(arrayOf(levelsIn[i].colorBuffer(0), levelsOut[i + 1].colorBuffer(0)), arrayOf(levelsOut[i].colorBuffer(0)))
        }

        passthrough.padding = -size - padding
        passthrough.apply(levelsOut[0].colorBuffer(0), result)
        passthrough.padding = 0
        return result
    }
}