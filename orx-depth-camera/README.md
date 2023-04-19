# orx-depth-camera

General [DepthCamera](src/commonMain/kotlin/DepthCamera.kt) API for obtaining the signal from any type of 
depth camera.

It offers selection of `DepthMeasurement` units (`METERS`, `RAW_NORMALIZED`, `RAW`), and allows
to flip the image horizontally and vertically.

Depth camera provides current resolution, current frame, as well as
an asynchronous hook to react to the latest received depth frame.

Note: Implementations will guarantee that the `onFrameReceived` hook is executed in coroutine of
the main drawer thread.

## Usage

Even though the API is generic, it can be used only with specific provider. The only one working at the moment:

 * [orx-kinect-v1](https://github.com/openrndr/orx/tree/master/orx-jvm/orx-kinect-v1)

```kotlin
import org.openrndr.application
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * Basic use case showing stream of depth camera frames.
 *
 * Note: kinect depth map is stored only on the RED color channel,
 * therefore depth map is displayed only in the red tones.
 */
fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        device.depthCamera.flipH = true // to make a mirror
        camera.depthMeasurement = DepthMeasurement.METERS        
        device.depthCamera.enabled = true
        extend {
            drawer.image(device.depthCamera.currentFrame)
        }
    }
}
```
