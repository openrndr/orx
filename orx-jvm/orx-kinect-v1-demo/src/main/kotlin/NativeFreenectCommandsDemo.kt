package org.openrndr.extra.kinect.v1.demo

import org.bytedeco.libfreenect.global.freenect
import org.bytedeco.libfreenect.global.freenect.*
import org.openrndr.application
import org.openrndr.extra.kinect.v1.getKinectsV1
import java.lang.RuntimeException

/**
 * Even though this library is abstracting freenect access, it is still
 * possible to call any low level kinect API through execute methods.
 * The calls are executed in separate kinect runner thread but they will
 * block the calling thread until the result is returned.
 */
fun main() = application {
    program {
        val kinects = getKinectsV1(this)
        // the same as calling kinects.countDevices(), here to show that any value might be returned from execute
        val num = kinects.execute { ctx -> freenect_num_devices(ctx.fnCtx) }
        if (num == 0) { throw RuntimeException("no kinect detected") }
        kinects.execute { ctx ->
            freenect_set_log_level(ctx.fnCtx, freenect.FREENECT_LOG_FLOOD) // lots of logs
        }
        kinects.execute { ctx ->
            // extra FREENECT_DEVICE_MOTOR gives control over tilt and LEDs
            freenect_select_subdevices(ctx.fnCtx, FREENECT_DEVICE_CAMERA xor FREENECT_DEVICE_MOTOR)
        }
        val kinect = kinects.startDevice()
        var tilt = 90.0
        extend {
            kinect.execute { ctx ->
                freenect_set_led(ctx.fnDev, (seconds * 10).toInt() % 7) // disco
            }
            val currentTilt = if ((seconds % 10) < 5) -90.0 else 90.0
            if (currentTilt != tilt) {
                kinect.execute { ctx ->
                    freenect_set_tilt_degs(ctx.fnDev, currentTilt)
                }
                tilt = currentTilt
            }
        }
    }
}
