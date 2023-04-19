import org.bytedeco.libfreenect.global.freenect
import org.bytedeco.libfreenect.global.freenect.*
import org.openrndr.application
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * Even though this library is abstracting freenect access, it is still
 * possible to call any low level kinect API through execute methods.
 * The calls are executed in separate kinect runner thread but they will
 * block the calling thread until the result is returned.
 */
fun main() = application {
    program {
        val kinect = extend(Kinect1())
        /*
         Blocking version will wait for the result, specifying the name
         makes it easier to identify this call in logs when it is finally
         executed on kinect. Note: enabling TRACE log level is required.
         */
        val numberOfKinectDevices = kinect.executeInFreenectContextBlocking(
            name = "numberOfKinectDevices"
        ) { ctx, _ ->
            freenect.freenect_num_devices(ctx)
        }
        println("numberOfKinectDevices: $numberOfKinectDevices")
        val device = kinect.openDevice()
        val maxTilt = 90.0
        var tilt = 0.0
        extend {
            device.executeInFreenectDeviceContext("disco LED") { _, _, dev ->
                freenect_set_led(dev, (seconds * 10).toInt() % 7) // disco
            }
            val newTilt = if ((seconds % 10) < 5) -maxTilt else maxTilt
            if (tilt != newTilt) {
                device.executeInFreenectDeviceContext("tilt change") { _, _, dev ->
                    freenect_set_tilt_degs(dev, tilt)
                }
                tilt = newTilt
            }
        }
    }
}
