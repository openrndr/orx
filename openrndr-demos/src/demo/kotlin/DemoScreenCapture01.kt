import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DepthTestPass
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extras.meshgenerators.boxMesh
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.Vector3

fun main() {
    application {
        program {
            val cube = boxMesh()
            val screen = VideoPlayerFFMPEG.fromScreen(
                frameRate = 15.0,
                imageWidth = 300,
                imageHeight = 300
            )

            screen.play()
            extend {
                screen.draw(drawer, true) // update the screen grabber
                drawer.isolated {
                    clear(ColorRGBa.WHITE)
                    perspective(60.0, width * 1.0 / height, 0.01, 1000.0)
                    depthWrite = true
                    depthTestPass = DepthTestPass.LESS_OR_EQUAL
                    shadeStyle = shadeStyle {
                        fragmentTransform = "x_fill = texture(p_tex, vec2(1.0-va_texCoord0.x, va_texCoord0.y));"
                        screen.colorBuffer?.run {
                            parameter("tex", this)
                        }
                    }
                    rotate(Vector3.UNIT_Z, 90.0)
                    translate(0.0, 0.0, -120.0)
                    rotate(Vector3.UNIT_X, seconds * 10)
                    scale(90.0)
                    vertexBuffer(cube, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}
