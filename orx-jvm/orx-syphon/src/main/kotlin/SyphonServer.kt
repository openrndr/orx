package jsyphon



import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget
import org.openrndr.internal.Driver

import org.openrndr.internal.gl3.ColorBufferGL3
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType


class SyphonServer(private val name: String = "OPENRNDR", var providedTarget: RenderTarget? = null): Extension {
    override var enabled = true
    private val server = JSyphonServer()
    private var targetToSend: RenderTarget? = null

    override fun setup(program: Program) {
        require(Driver.glType == DriverTypeGL.GL) {
            "The SyphonServer extension will only work when using OpenGL. Use -Dorg.openrndr.gl3.gl_type=gl to force OPENRNDR to use OpenGL."
        }

        server.initWithName(name)

        // Create a new target that binds to the main one if no target is provided
        if (providedTarget == null) {
            targetToSend = renderTarget(program.width, program.height) {
                colorBuffer()
                depthBuffer()
            }
        } else {
            targetToSend = providedTarget
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (providedTarget == null) {
            targetToSend?.bind()
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        if (providedTarget == null) {
            targetToSend?.unbind()
            // Actually draw it, necessary because of bind().
            // Only draw if it's the main target.
            drawer.image(targetToSend?.colorBuffer(0)!!)
        }

        val glBuffer = targetToSend?.colorBuffer(0) as ColorBufferGL3

        targetToSend?.let { targetToSend ->
            // Send to Syphon
            server.publishFrameTexture(
                glBuffer.texture, glBuffer.target, 0, 0,
                targetToSend.width, targetToSend.height, targetToSend.width, targetToSend.height, false
            )
        }
    }

    override fun shutdown(program: Program) {
        // Cleanup
        server.stop()
    }
}






