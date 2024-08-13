package org.openrndr.extra.fx

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

class Post : Extension {
    override var enabled = true

    private var inputTarget: RenderTarget? = null
    private var resolved: ColorBuffer? = null

    /**
     * The color type to use for the intermediate color buffers
     */
    var intermediateType = ColorType.UINT8_SRGB

    /**
     * The color type to use for the output color buffer
     */
    var outputType = ColorType.UINT8_SRGB

    /**
     * The color type to use for the input buffer
     */
    var inputType = ColorType.UINT8_SRGB

    /**
     * The depth format to use for the input buffer
     */
    var inputDepthFormat = DepthFormat.DEPTH_STENCIL

    private var output: ColorBuffer? = null
    private var postFunction = { input: ColorBuffer, output: ColorBuffer -> input.copyTo(output) }

    inner class IntermediateBuffers {
        internal val buffers = mutableMapOf<Int, ColorBuffer>()
        operator fun get(index: Int): ColorBuffer {
            return buffers.getOrPut(index) {
                colorBuffer(output!!.width, output!!.height, output!!.contentScale, type = intermediateType)
            }
        }
    }

    /**
     * Intermediate buffer pool, provides automatically allocated color buffers
     */
    val intermediate = IntermediateBuffers()

    /**
     * Set the post function
     * @param function the post function
     */
    fun post(function: (input: ColorBuffer, output: ColorBuffer) -> Unit) {
        postFunction = function
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        val art = RenderTarget.active
        val lit = inputTarget
        if (lit != null) {
            // in case the attributes of the existing buffers no longer match those of the active render target
            if (lit.width != art.width || lit.height != art.height || lit.contentScale != art.contentScale || lit.multisample != art.multisample) {
                lit.colorBuffer(0).destroy()
                lit.depthBuffer?.destroy()
                lit.detachDepthBuffer()
                lit.detachColorAttachments()
                lit.destroy()
                inputTarget = null

                resolved?.destroy()
                resolved = null

                output?.destroy()
                output = null

                for (buffer in intermediate.buffers.values) {
                    buffer.destroy()
                }
                intermediate.buffers.clear()
            }
        }
        if (inputTarget == null) {
            // create new targets and buffers
            inputTarget = renderTarget(art.width, art.height, art.contentScale, multisample = art.multisample) {
                colorBuffer(type = inputType)
                depthBuffer(format = inputDepthFormat)
            }
            if (art.multisample != BufferMultisample.Disabled) {
                resolved = colorBuffer(art.width, art.height, art.contentScale)
            }
            output = colorBuffer(art.width, art.height, art.contentScale, type = outputType)
        }
        // bind input target, the next extensions will draw into it
        inputTarget!!.bind()
        drawer.clear(ColorRGBa.TRANSPARENT)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        inputTarget!!.unbind()

        if (resolved != null) {
            inputTarget!!.colorBuffer(0).copyTo(resolved!!)
        }

        val postInput = resolved ?: inputTarget!!.colorBuffer(0)

        // invoke the user provided post-processing function
        postFunction(postInput, output!!)

        // visualize the results
        drawer.isolated {
            drawer.defaults()
            drawer.image(output!!)
        }
    }
}