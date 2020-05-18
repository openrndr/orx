# orx-kinect-v1

Support for the Kinect V1 RGB+Depth camera.

## Example usage

```
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
