import org.openrndr.application
import org.openrndr.draw.Filter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * It is possible to rewrite raw kinect value interpretation completely
 * while keeping all the performance characteristics.
 *
 * Note: when depth measurement is set to RAW, the flip options does not apply.
 */
fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val camera = device.depthCamera
        camera.depthMeasurement = DepthMeasurement.RAW
        val outputBuffer = colorBuffer(camera.resolution.x, camera.resolution.y)
        val filter = Filter(filterShaderFromCode("""
            layout(origin_upper_left) in vec4 gl_FragCoord;            
            uniform usampler2D  tex0;             // kinect raw
            out     vec4        o_color;

            void main() {
                ivec2 uv = ivec2(gl_FragCoord);
                uint uintDepth = texelFetch(tex0, uv, 0).r;
                float depth = float(uintDepth) / 2047.;
                o_color = vec4(vec3(depth), 1.);
            }
            """.trimIndent(),
            "raw filter")
        )
        camera.onFrameReceived { frame ->
            filter.apply(frame, outputBuffer)
        }
        device.depthCamera.enabled = true
        extend {
            drawer.image(outputBuffer)
        }
    }
}
