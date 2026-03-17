package org.openrndr.extra.noclear

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44

class NoClear(val multisample: BufferMultisample = BufferMultisample.Disabled) : Extension {
    override var enabled: Boolean = true
    private var renderTarget: RenderTarget? = null
    private var resolvedColorBuffer: ColorBuffer? = null

    /**
     * Defines the type of the color buffer used for rendering operations within the `NoClear` class.
     *
     * The default value is `ColorType.UINT8_SRGB`, which represents an 8-bit unsigned integer
     * per channel in standard RGB color space with sRGB gamma correction.
     */
    var colorType = ColorType.UINT8_SRGB

    /**
     * code-block to draw an optional custom backdrop
     */
    var backdrop: (() -> Unit)? = null

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (program.width > 0 && program.height > 0) {    // only if the window is not minimised
            if (renderTarget == null || renderTarget?.width != program.width || renderTarget?.height != program.height) {
                renderTarget?.destroy()

                renderTarget = renderTarget(program.width, program.height, program.window.contentScale, multisample) {
                    colorBuffer(type = colorType)
                    depthBuffer()
                }

                if (multisample != BufferMultisample.Disabled) {
                    resolvedColorBuffer?.destroy()
                    resolvedColorBuffer = colorBuffer(program.width, program.height, program.window.contentScale)
                }

                renderTarget?.let {
                    drawer.withTarget(it) {
                        clear(program.backgroundColor ?: ColorRGBa.TRANSPARENT)
                        backdrop?.invoke() // draw custom backdrop
                    }
                }
            }
        }
        renderTarget?.bind()
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        renderTarget?.unbind()

        renderTarget?.let {
            drawer.isolated {
                drawer.ortho()
                drawer.view = Matrix44.IDENTITY
                drawer.model = Matrix44.IDENTITY
                if (multisample != BufferMultisample.Disabled) {
                    it.colorBuffer(0).copyTo(resolvedColorBuffer!!)
                    drawer.image(resolvedColorBuffer!!)
                } else {
                    drawer.image(it.colorBuffer(0))
                }
            }
        }
    }
}