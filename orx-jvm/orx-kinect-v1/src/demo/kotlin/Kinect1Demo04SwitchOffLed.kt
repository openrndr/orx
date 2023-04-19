import org.bytedeco.libfreenect.global.freenect
import org.openrndr.application
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * This demo shows how to execute freenect commands directly, either globally
 * or on the device. In this case demo is switching off LED light completely,
 * which might be desirable for the aesthetics of an installation,
 * however LED turned on might be still a useful indicator during development.
 */
fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        device.executeInFreenectDeviceContext(
            "turn off led"
        ) { _, _, dev ->
            freenect.freenect_set_led(dev, freenect.LED_OFF)
        }
        device.depthCamera.enabled = true
        extend {
            drawer.image(device.depthCamera.currentFrame)
        }
    }
}
