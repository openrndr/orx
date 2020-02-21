import jsyphon.JSyphonClient
import jsyphon.JSyphonImage
import org.lwjgl.opengl.GL11C.GL_TEXTURE_2D
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.ColorBufferGL3
import kotlin.math.sin


fun main() = application {
    configure {
        width = 1000
        height = 1000
    }

    program {
        val client = JSyphonClient()
        client.init()


        extend {
            drawer.background(ColorRGBa.RED)

            if (client.hasNewFrame()) {
                println("hasnewframe")
                val img = client.newFrameImageForContext()
                val buffer = ColorBufferGL3(GL_TEXTURE_2D, img.textureName(), img.textureWidth(), img.textureHeight(),
                                                1.0, ColorFormat.RGBa, ColorType.UINT8, 1, BufferMultisample.Disabled, Session.active)


                drawer.image(buffer)
            }

        }
    }
}