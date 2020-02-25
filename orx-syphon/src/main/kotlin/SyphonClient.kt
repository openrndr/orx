import jsyphon.JSyphonClient
import jsyphon.JSyphonServer
import org.lwjgl.opengl.GL33C
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.internal.gl3.ColorBufferGL3


class SyphonClient: Extension {
    override var enabled = true

    private val client = JSyphonClient()
    var buffer: ColorBuffer = colorBuffer(10, 10)

    override fun setup(program: Program) {
        var buffer = colorBuffer(program.width, program.height)
        client.init()
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (client.hasNewFrame()) {
            val img = client.newFrameImageForContext()
            val w = img.textureWidth()
            val h = img.textureHeight()

            val rectBuffer = ColorBufferGL3(GL33C.GL_TEXTURE_RECTANGLE, img.textureName(), w, h,
                    1.0, ColorFormat.RGBa, ColorType.UINT8, 0, BufferMultisample.Disabled, Session.root)

            if (buffer.height != h || buffer.width != w) {
                buffer  = colorBuffer(w, h)
            }

            rectBuffer.copyTo(buffer)
        }
    }
}


