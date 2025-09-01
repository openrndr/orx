package jsyphon



import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.internal.Driver


import org.openrndr.internal.gl3.ColorBufferGL3
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType


class SyphonClient(private val appName: String? = null, private val serverName: String? = null): Extension {
    override var enabled = true

    private val client = JSyphonClient()
    var buffer: ColorBuffer = colorBuffer(10, 10)

    override fun setup(program: Program) {
        require(Driver.glType == DriverTypeGL.GL) {
            "The SyphonClient extension will only work when using OpenGL. Use -Dorg.openrndr.gl3.gl_type=gl to force OPENRNDR to use OpenGL."
        }

        buffer = colorBuffer(program.width, program.height)

        client.init()

        // Choosing a different server
        if (appName != null) client.setApplicationName(appName)
        if (serverName != null) client.setServerName(serverName)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (client.hasNewFrame()) {
            val img = client.newFrameImageForContext()
            val name = img.textureName()
            val w = img.textureWidth()
            val h = img.textureHeight()



            /**
             * GL_TEXTURE_RECTANGLE is necessary
             */
            val GL_TEXTURE_RECTANGLE = 0x84F5
                val rectBuffer = ColorBufferGL3(GL_TEXTURE_RECTANGLE, name, w, h, 1.0, ColorFormat.RGBa, ColorType.UINT8, 0, BufferMultisample.Disabled, Session.root)

            /**
             * Only create a new buffer if it's size changed
             */
            if (buffer.height != h || buffer.width != w) {
                buffer  = colorBuffer(w, h)
            }

            rectBuffer.copyTo(buffer)
        }
    }

    override fun shutdown(program: Program) {
        client.stop()
    }
}


