
import jsyphon.JSyphonServer
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget
import org.openrndr.internal.gl3.ColorBufferGL3


class SyphonServer(private val name: String = "OPENRNDR", var target: RenderTarget? = null): Extension {
    override var enabled = true
    private val server = JSyphonServer()

    override fun setup(program: Program) {
        server.initWithName(name)

        if (target == null) {
            target = renderTarget(program.width, program.height) {
                colorBuffer()
                depthBuffer()
            }
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        target?.bind()
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        target?.unbind()
        drawer.image(target?.colorBuffer(0)!!)
        val glBuffer = target?.colorBuffer(0) as ColorBufferGL3

        println(glBuffer.target)

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






