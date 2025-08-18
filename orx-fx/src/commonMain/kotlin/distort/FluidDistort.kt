package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_fluid_distort
import org.openrndr.extra.fx.fx_uvmap
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.shape.Rectangle
import kotlin.math.cos

private class UVMap: Filter( mppFilterShader(fx_uvmap, "uvmap"))

private class FluidDistortFilter : Filter(mppFilterShader(fx_fluid_distort, "fluid-distort")) {
    var blend : Double by parameters
    var random: Double by parameters
    init {
        blend = 0.0
        random = 0.0
    }
}

class FluidDistort : Filter1to1(null) {
    var blend: Double = 1.0

    var outputUV = false

    private val distort = FluidDistortFilter()
    private val uvmap = UVMap()

    private var buffer0: ColorBuffer? = null
    private var buffer1: ColorBuffer? = null
    private var index = 0
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)
        distort.blend = blend
        distort.random = cos(index*0.5)*0.5+0.5

        buffer0?.let {
            if (!it.isEquivalentTo(target[0])) {
                it.destroy()
            }
        }
        if (buffer0 == null) {
            buffer0 = target[0].createEquivalent()
        }

        buffer1?.let {
            if (!it.isEquivalentTo(target[0])) {
                it.destroy()
            }
        }
        if (buffer1 == null) {
            buffer1 = target[0].createEquivalent()
        }
        val buffers = arrayOf(buffer0!!, buffer1!!)
        distort.apply(buffers[index%2], buffers[(index+1)%2], clip)

        if (!outputUV) {
            uvmap.apply(arrayOf(buffers[(index + 1) % 2], source[0]), target[0], clip)
        } else {
            buffers[(index+1)%2]. copyTo(target[0])
        }
        index++
        blend = 0.0
    }

}