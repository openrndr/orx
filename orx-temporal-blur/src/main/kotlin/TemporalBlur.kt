package org.openrndr.extra.temporalblur

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.fx.blend.Add
import org.openrndr.filter.color.delinearize
import org.openrndr.filter.color.linearize
import org.openrndr.math.Matrix44
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.translate

private val add by lazy { Add() }

/**
 * Temporal blur extension.
 * This works best in video rendering applications as it heavily relies on rendering
 * the scene as many times as you have samples.
 */
class TemporalBlur : Extension {
    override var enabled: Boolean = true

    private var accumulator: RenderTarget? = null
    private var result: RenderTarget? = null
    private var image: RenderTarget? = null
    private var imageResolved: RenderTarget? = null

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

    override fun beforeDraw(drawer: Drawer, program: Program) {
        val extensionOffset = program.extensions.indexOf(this)
        val extensionTail = program.extensions.drop(extensionOffset + 1)

        accumulator?.let { a ->
            if (a.width != program.width || a.height != program.height) {
                a.colorBuffer(0).destroy()
                a.detachColorBuffers()
                a.destroy()
            }
        }

        result?.let { r ->
            if (r.width != program.width || r.height != program.height) {
                r.colorBuffer(0).destroy()
                r.detachColorBuffers()
                r.destroy()
            }
        }

        image?.let { i ->
            if (i.width != program.width || i.height != program.height) {
                i.colorBuffer(0).destroy()
                i.depthBuffer?.destroy()
                i.detachColorBuffers()
                i.detachDepthBuffer()
                i.destroy()
            }
        }

        imageResolved?.let { i ->
            if (i.width != program.width || i.height != program.height) {
                i.colorBuffer(0).destroy()
                i.detachColorBuffers()
                i.destroy()
            }
        }

        if (accumulator == null) {
            accumulator = renderTarget(program.width, program.height) {
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        if (result == null) {
            result = renderTarget(program.width, program.height) {
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        if (image == null) {
            image = renderTarget(program.width, program.height, multisample = BufferMultisample.SampleCount(8)) {
                depthBuffer()
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        if (imageResolved == null) {
            imageResolved = renderTarget(program.width, program.height) {
                depthBuffer()
                colorBuffer(type = ColorType.FLOAT32)
            }
        }

        accumulator?.let {
            drawer.withTarget(it) {
                drawer.clear(ColorRGBa.BLACK)
            }
        }
        val oldClock = program.clock
        val oldClockValue = oldClock()

        for (i in samples - 1 downTo 1) {
            image?.bind()

            drawer.clear(ColorRGBa.BLACK)
            program.clock = { oldClockValue - (i * duration) / (fps * samples) }

            // I guess we need something better here.
            val fsf = Program::class.java.getDeclaredField("frameSeconds")
            fsf.isAccessible = true
            fsf.setDouble(program, program.clock())

            drawer.drawStyle.blendMode = BlendMode.OVER
            drawer.drawStyle.colorMatrix = Matrix55.IDENTITY
            drawer.isolated {
                val offset = Vector2.uniformRing(0.0, jitter)
                drawer.projection = Matrix44.translate(offset.x * (1.0 / program.width), offset.y * (1.0 / program.height), 0.0) * drawer.projection

                for (extension in extensionTail) {
                    extension.beforeDraw(drawer, program)
                }

                for (extension in extensionTail.reversed())
                    extension.afterDraw(drawer, program)
            }

            image?.unbind()
            image!!.colorBuffer(0).resolveTo(imageResolved!!.colorBuffer(0))

            if (linearizeInput) {
                imageResolved?.let {
                    linearize.apply(it, it)
                }
            }

            add.apply(arrayOf(imageResolved!!.colorBuffer(0), accumulator!!.colorBuffer(0)), accumulator!!.colorBuffer(0))
            program.clock = oldClock
        }
        image?.let {
            drawer.withTarget(it) {
                drawer.clear(ColorRGBa.BLACK)
            }
        }
        image?.bind()
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        // -- we receive one last frame here
        image?.unbind()
        image!!.colorBuffer(0).resolveTo(imageResolved!!.colorBuffer(0))

        add.apply(arrayOf(imageResolved!!.colorBuffer(0), accumulator!!.colorBuffer(0)), accumulator!!.colorBuffer(0))

        // -- render accumulated result
        drawer.isolated {
            drawer.ortho(result!!)
            drawer.model = Matrix44.IDENTITY
            drawer.view = Matrix44.IDENTITY

            drawer.isolatedWithTarget(result!!) {
                drawer.drawStyle.blendMode = BlendMode.OVER

                drawer.clear(ColorRGBa.BLACK)
                drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade(1.0 / samples))
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