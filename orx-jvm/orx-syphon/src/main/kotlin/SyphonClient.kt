package org.openrndr.extra.syphon



import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.extra.syphon.jsyphon.JSyphonClient

import org.openrndr.internal.gl3.ColorBufferGL3
import org.openrndr.internal.gl3.TextureStorageModeGL


class SyphonClient(private val appName: String? = null, private val serverName: String? = null): Extension {
    override var enabled = true

    private val client = JSyphonClient()
    var buffer: ColorBuffer = colorBuffer(10, 10)

    override fun setup(program: Program) {
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
            val rectBuffer = ColorBufferGL3(GL_TEXTURE_RECTANGLE, name, TextureStorageModeGL.IMAGE, w, h, 1.0, ColorFormat.RGBa, ColorType.UINT8, 0, BufferMultisample.Disabled, Session.root)

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


