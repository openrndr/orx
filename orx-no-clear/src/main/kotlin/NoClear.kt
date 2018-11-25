package org.openrndr.extra.noclear

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolated
import org.openrndr.draw.renderTarget
import org.openrndr.math.Matrix44

class NoClear(val block: (() -> Unit)? = null): Extension {
    override var enabled: Boolean = true
    private var renderTarget: RenderTarget? = null

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (program.width > 0 && program.height > 0) {    // only if the window is not minimised
            if (renderTarget == null || renderTarget?.width != program.width || renderTarget?.height != program.height) {
                renderTarget?.let {
                    it.colorBuffer(0).destroy()
                    it.detachColorBuffers()
                    it.destroy()
                }
                renderTarget = renderTarget(program.width, program.height) {
                    colorBuffer()
                    depthBuffer()
                }

                renderTarget?.let {
                    drawer.withTarget(it) {
                        background(program.backgroundColor ?: ColorRGBa.TRANSPARENT)
                        block?.invoke()
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
                drawer.image(it.colorBuffer(0))
            }
        }
    }
}