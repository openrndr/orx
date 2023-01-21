package org.openrndr.extra.temporalblur

import org.intellij.lang.annotations.Language
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.ProgramImplementation
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.uniformRing
import org.openrndr.filter.color.delinearize
import org.openrndr.filter.color.linearize
import org.openrndr.math.Matrix44
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.translate


private fun glslFull(@Language("GLSL") glsl: String) = glsl

class PlainAdd : Filter(filterShaderFromCode(glslFull("""

#version 330

uniform sampler2D tex0;
uniform sampler2D tex1;
in vec2 v_texCoord0;
out vec4 o_output;

void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    o_output = a + b;    
}
    
    
"""), "plain-add"))


private val add by lazy { PlainAdd() }

/**
 * Temporal blur extension.
 * This works best in video rendering applications as it heavily relies on rendering
 * the scene as many times as you have samples.
 */
class TemporalBlur : Extension {
    private var oldClock: () -> Double = { 0.0 }
    var oldClockTime = 0.0
    override var enabled: Boolean = true

    private var accumulator: RenderTarget? = null
    private var result: RenderTarget? = null
    private var image: RenderTarget? = null
    private var imageResolved: RenderTarget? = null

    var contentScale : Double? = null

    // modifier for final stage averager, higher gain results in brighter images
    var gain = 1.0

    /**
     * number of samples to take, more is slower
     */
    var samples = 30

    /**
     * duration in frames, shouldn't be 1.0 or larger when using Animatables
     */
    var duration = 0.5

    /**
     * reference frame rate
     */
    var fps = 60.0

    /**
     * spatial jitter in pixels, 0 is no jitter
     */
    var jitter = 1.0

    /**
     * should the accumulator linearize the input. this should be true when rendering in sRGB
     */
    var linearizeInput = true

    /**
     * should the accumulator delinearize the output. this should be true when rendering in sRGB
     */
    var delinearizeOutput = true

    /**
     * multisampling setting
     * */
    var multisample: BufferMultisample = BufferMultisample.SampleCount(8)


    var colorMatrix: (Double)->Matrix55 = { Matrix55.IDENTITY }


    var beforeDrawAccumulated : TemporalBlur.() -> Unit = {}

    override fun beforeDraw(drawer: Drawer, program: Program) {
        val extensionOffset = program.extensions.indexOf(this)
        val extensionTail = program.extensions.drop(extensionOffset + 1)

        accumulator?.let { a ->
            if (a.width != program.width || a.height != program.height) {
                a.colorBuffer(0).destroy()
                a.detachColorAttachments()
                a.destroy()
            }
        }

        result?.let { r ->
            if (r.width != program.width || r.height != program.height) {
                r.colorBuffer(0).destroy()
                r.detachColorAttachments()
                r.destroy()
            }
        }

        image?.let { i ->
            if (i.width != program.width || i.height != program.height) {
                i.colorBuffer(0).destroy()
                i.depthBuffer?.destroy()
                i.detachColorAttachments()
                i.detachDepthBuffer()
                i.destroy()
            }
        }

        imageResolved?.let { i ->
            if (i.width != program.width || i.height != program.height) {
                i.colorBuffer(0).destroy()
                i.detachColorAttachments()
                i.destroy()
            }
        }

        val resolvedContentScale = contentScale ?: RenderTarget.active.contentScale

        if (accumulator == null) {
            accumulator = renderTarget(program.width, program.height, contentScale = resolvedContentScale) {
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        if (result == null) {
            result = renderTarget(program.width, program.height, contentScale = resolvedContentScale) {
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        if (image == null) {
            image = renderTarget(program.width, program.height, multisample = multisample, contentScale = resolvedContentScale) {
                depthBuffer()
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        if (imageResolved == null) {
            imageResolved = renderTarget(program.width, program.height, contentScale = resolvedContentScale) {
                depthBuffer()
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        accumulator?.let {
            drawer.withTarget(it) {
                drawer.clear(ColorRGBa.BLACK)
            }
        }
        oldClock = program.clock
        oldClockTime = program.clock()
        val oldClockValue = oldClock()

        for (i in samples - 1 downTo 1) {
            image?.bind()
            drawer.clear(ColorRGBa.BLACK)
            program.clock = { oldClockValue - (i * duration) / (fps * samples) }

            // I guess we need something better here.
            val fsf = ProgramImplementation::class.java.getDeclaredField("frameSeconds")
            fsf.isAccessible = true
            fsf.setDouble(program, program.clock())

            drawer.drawStyle.blendMode = BlendMode.OVER
            drawer.drawStyle.colorMatrix = Matrix55.IDENTITY
            drawer.isolated {
                if (jitter > 0.0){
                    val offset = Vector2.uniformRing(0.0, jitter)
                    drawer.projection = Matrix44.translate(offset.x * (1.0 / program.width), offset.y * (1.0 / program.height), 0.0) * drawer.projection
                }

                for (extension in extensionTail) {
                    extension.beforeDraw(drawer, program)
                }

                for (extension in extensionTail.reversed())
                    extension.afterDraw(drawer, program)
            }

            image?.unbind()
            image!!.colorBuffer(0).copyTo(imageResolved!!.colorBuffer(0))

            if (linearizeInput) {
                imageResolved?.let {
                    linearize.apply(it.colorBuffer(0), it.colorBuffer(0))
                }
            }

            val activeColorMatrix = colorMatrix(i / (samples - 1.0))
            if (activeColorMatrix !== Matrix55.IDENTITY) {
                drawer.isolatedWithTarget(imageResolved!!) {
                    drawer.drawStyle.colorMatrix = activeColorMatrix
                    drawer.drawStyle.blendMode = BlendMode.REPLACE
                    drawer.image(imageResolved!!.colorBuffer(0))
                }
            }
            add.apply(arrayOf(imageResolved!!.colorBuffer(0), accumulator!!.colorBuffer(0)), accumulator!!.colorBuffer(0))

            fsf.setDouble(program, program.clock())
        }
        image?.let {
            drawer.withTarget(it) {
                drawer.clear(ColorRGBa.BLACK)
            }
        }
        image?.bind()

        // restore clock
        program.clock = oldClock
        val fsf = ProgramImplementation::class.java.getDeclaredField("frameSeconds")
        fsf.isAccessible = true
        fsf.setDouble(program,  oldClockTime)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        // -- we receive one last frame here
        image?.unbind()
        image!!.colorBuffer(0).copyTo(imageResolved!!.colorBuffer(0))

        val activeColorMatrix = colorMatrix(0.0)
        if (activeColorMatrix !== Matrix55.IDENTITY) {
            drawer.isolatedWithTarget(imageResolved!!) {
                drawer.drawStyle.colorMatrix = activeColorMatrix
                drawer.drawStyle.blendMode = BlendMode.REPLACE
                drawer.image(imageResolved!!.colorBuffer(0))
            }
        }

        add.apply(arrayOf(imageResolved!!.colorBuffer(0), accumulator!!.colorBuffer(0)), accumulator!!.colorBuffer(0))
        beforeDrawAccumulated()

        // -- render accumulated result
        drawer.isolated {
            drawer.defaults()
            drawer.ortho(result!!)

            drawer.isolatedWithTarget(result!!) {
                drawer.drawStyle.blendMode = BlendMode.OVER

                drawer.clear(ColorRGBa.BLACK)
                drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade((1.0 / samples) * gain))
                drawer.image(accumulator!!.colorBuffer(0))
            }
            if (delinearizeOutput) {
                delinearize.apply(result!!.colorBuffer(0), result!!.colorBuffer(0))
            }
            drawer.drawStyle.blendMode = BlendMode.OVER
            drawer.drawStyle.colorMatrix = Matrix55.IDENTITY
            drawer.drawStyle.depthTestPass = DepthTestPass.ALWAYS

            drawer.clear(ColorRGBa.BLACK)
            drawer.image(result!!.colorBuffer(0))
        }
    }
}