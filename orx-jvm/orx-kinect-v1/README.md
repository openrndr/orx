# orx-kinect-v1

Support for the Kinect V1 RGB and depth cameras.

If using Linux, add the [udev rules](https://github.com/OpenKinect/libfreenect/tree/master/platform/linux/udev) to be able to access the camera without being a root user.

## Example usage

```kotlin
fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
    program {
        val kinects = getKinectsV1()
        val kinect = kinects.startDevice()
        kinect.depthCamera.enabled = true
        kinect.depthCamera.mirror = true
        extend(kinect)
        extend {
            drawer.image(kinect.depthCamera.currentFrame)
        }
    }
}
```
