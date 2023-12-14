@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_laser_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.pow

private class LaserBlurPass : Filter(mppFilterShader(fx_laser_blur, "laser-blur")) {
    var radius: Double by parameters
    var amp0: Double by parameters
    var amp1: Double by parameters
    var center: Vector2 by parameters
    var vignette: Double by parameters
    var vignetteSize: Double by parameters
    var aberration: Double by parameters
    var linearInput: Boolean by parameters
    var linearOutput: Boolean by parameters

    init {
        radius = 0.0
        amp0 = 1.0
        amp1 = 1.0
        center = Vector2.ZERO
        vignette = 0.0
        vignetteSize = 1.0
        aberration = 0.0
        linearInput = false
        linearOutput = false
    }
}

@Description("Laser blur")
class LaserBlur : Filter1to1() {
    @Vector2Parameter("center", order = 0)
    var center = Vector2.ZERO

    @DoubleParameter("radius", -2.0, 2.0, order = 1)
    var radius = -0.18

    @DoubleParameter("amp0", 0.0, 1.0, order = 2)
    var amp0 = 0.5

    @DoubleParameter("amp1", 0.0, 1.0, order = 3)
    var amp1 = 0.5

    @DoubleParameter("vignette", 0.0, 1.0, order = 4)
    var vignette = 0.0

    @DoubleParameter("vignette size", 0.0, 1.0, order = 5)
    var vignetteSize = 0.0

    @DoubleParameter("aberration", -1.0, 1.0, order = 6)
    var aberration = 0.006

    @DoubleParameter("exp", -1.0, 1.0, order = 7)
    var exp = 0.739

    @BooleanParameter("linear input", order = 8)
    var linearInput = false

    @BooleanParameter("linear output", order = 9)
    var linearOutput = false

    @DoubleParameter("phase", -1.0, 1.0, order = 7)
    var phase = 0.0


    private val pass = LaserBlurPass()

    @IntParameter("passes", 2, 32, order = 4)
    var passes = 15

    val intermediates = mutableListOf<ColorBuffer>()

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip:Rectangle?) {
        pass.center = center
        pass.radius = radius
        pass.amp0 = amp0
        pass.amp1 = amp1
        pass.vignette = vignette
        pass.vignetteSize = vignetteSize
        pass.aberration = aberration

        if ((!intermediates.all { it.isEquivalentTo(source[0], ignoreFormat = true, ignoreType = true) })) {
            intermediates.forEach {
                it.destroy()
            }
            intermediates.clear()
        }
        if (intermediates.size == 0) {
            intermediates.add(source[0].createEquivalent(type = ColorType.FLOAT16))
            intermediates.add(source[0].createEquivalent(type = ColorType.FLOAT16))
        }

        pass.radius = 1.0 + pow(exp, 0.0) * radius

        pass.linearInput = linearInput
        pass.linearOutput = true
        pass.apply(source[0], intermediates[0], clip)
        for (i in 0 until passes - 1) {
            pass.linearInput = true
            pass.linearOutput = true

            pass.radius = 1.0 + pow(exp, i + 1.0) * radius //(1.0 + simplex(0, phase + i)) / 2.0
            pass.apply(intermediates[i % 2], intermediates[(i + 1) % 2], clip)
        }
        pass.radius = 1.0 + pow(exp, (passes) * 1.0) * radius
        pass.linearInput = true
        pass.linearOutput = linearOutput
        pass.apply(intermediates[(passes + 1) % 2], target[0], clip)
    }
}

private fun pow(a: Double, x: Double): Double {
    return a.pow(x)
}