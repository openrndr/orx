
import jsyphon.JSyphonServer
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget
import org.openrndr.internal.gl3.ColorBufferGL3


class SyphonServer(private val name: String = "OPENRNDR", var providedTarget: RenderTarget? = null): Extension {
    override var enabled = true
    private val server = JSyphonServer()
    private var targetToSend: RenderTarget? = null

    override fun setup(program: Program) {
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

        // Send to Syphon
        server.publishFrameTexture(
            glBuffer.texture, glBuffer.target, 0, 0,
            program.width, program.height, program.width, program.height, false
        )
    }

    override fun shutdown(program: Program) {
        // Cleanup
        server.stop()
    }
}






