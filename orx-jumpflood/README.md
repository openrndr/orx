# orx-jumpflood

Takes an image and calculates either a distance field or a direction field. 
GPU accelerated, 2D. Results are provided as an image.

[Original jump flooding algorithm](https://www.comp.nus.edu.sg/~tants/jfa.html)

`orx-jumpflood` focusses on finding 2d distance and directional distance fields.

## Distance field example

`distanceFieldFromBitmap()` calculates distances to bitmap contours it stores
the distance in red and the original bitmap in green.


```kotlin
import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.Threshold
import org.openrndr.extra.jumpfill.distanceFieldFromBitmap
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.filter.blur.ApproximateGaussianBlur

fun main() = application {
    configure {
        width = 1280
        height = 720
    }

    program {
        val blurFilter = ApproximateGaussianBlur()
        val blurred = colorBuffer(width, height)

        val thresholdFilter = Threshold()
        val thresholded = colorBuffer(width, height)

        val distanceField = colorBuffer(width, height, type = ColorType.FLOAT32)

        val videoCopy = renderTarget(width, height) {
            colorBuffer()
        }
        val videoPlayer = VideoPlayerFFMPEG.fromDevice(imageWidth = width, imageHeight = height)
        videoPlayer.play()

        extend {
            // -- copy videoplayer output
            drawer.isolatedWithTarget(videoCopy) {
                drawer.ortho(videoCopy)
                videoPlayer.draw(drawer)
            }

            // -- blur the input a bit, this produces less noisy bitmap images
            blurFilter.sigma = 9.0
            blurFilter.window = 18
            blurFilter.apply(videoCopy.colorBuffer(0), blurred)

            // -- threshold the blurred image
            thresholdFilter.threshold = 0.5
            thresholdFilter.apply(blurred, thresholded)

            distanceFieldFromBitmap(thresholded, result = distanceField)

            drawer.isolated {
                // -- use a shadestyle to visualize the distance field
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        float d = x_fill.r;
                        if (x_fill.g > 0.5) { 
                            x_fill.rgb = vec3(cos(d) * 0.5 + 0.5);
                        } else {
                            x_fill.rgb = 0.25 * vec3(1.0 - (cos(d) * 0.5 + 0.5));
                        }
                    """
                }
                drawer.image(distanceField)
            }
        }
    }
}
```

## Direction field example

`directionFieldFromBitmap()` calculates directions to bitmap contours it stores
x-direction in red, y-direction in green, and the original bitmap in blue.


```kotlin
import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.Threshold
import org.openrndr.extra.jumpfill.directionFieldFromBitmap
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.filter.blur.ApproximateGaussianBlur

fun main() = application {
    configure {
        width = 1280
        height = 720
    }

    program {
        val blurFilter = ApproximateGaussianBlur()
        val blurred = colorBuffer(width, height)

        val thresholdFilter = Threshold()
        val thresholded = colorBuffer(width, height)

        val directionField = colorBuffer(width, height, type = ColorType.FLOAT32)

        val videoPlayer = VideoPlayerFFMPEG.fromDevice(imageWidth = width, imageHeight = height)
        videoPlayer.play()

        val videoCopy = renderTarget(width, height) {
            colorBuffer()
        }

        extend {
            // -- copy videoplayer output
            drawer.isolatedWithTarget(videoCopy) {
                drawer.ortho(videoCopy)
                videoPlayer.draw(drawer)
            }

            // -- blur the input a bit, this produces less noisy bitmap images
            blurFilter.sigma = 9.0
            blurFilter.window = 18
            blurFilter.apply(videoCopy.colorBuffer(0), blurred)

            // -- threshold the blurred image
            thresholdFilter.threshold = 0.5
            thresholdFilter.apply(blurred, thresholded)

            directionFieldFromBitmap(thresholded, result = directionField)

            drawer.isolated {
                // -- use a shadestyle to visualize the direction field
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                        float a = atan(x_fill.r, x_fill.g);
                        if (x_fill.b > 0.5) { 
                            x_fill.rgb = vec3(cos(a)*0.5+0.5, 1.0, sin(a)*0.5+0.5);
                        } else {
                            x_fill.rgb = vec3(cos(a)*0.5+0.5, 0.0, sin(a)*0.5+0.5);
                        }
                    """
                }
                drawer.image(directionField)
            }
        }
    }
}
```
<!-- __demos__ -->
## Demos
### DemoInnerGlow01
[source code](src/demo/kotlin/DemoInnerGlow01.kt)

![DemoInnerGlow01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoInnerGlow01Kt.png)

### DemoInnerGlow02
[source code](src/demo/kotlin/DemoInnerGlow02.kt)

![DemoInnerGlow02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoInnerGlow02Kt.png)

### DemoShapeSDF01
[source code](src/demo/kotlin/DemoShapeSDF01.kt)

![DemoShapeSDF01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF01Kt.png)

### DemoShapeSDF02
[source code](src/demo/kotlin/DemoShapeSDF02.kt)

![DemoShapeSDF02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF02Kt.png)

### DemoShapeSDF03
[source code](src/demo/kotlin/DemoShapeSDF03.kt)

![DemoShapeSDF03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF03Kt.png)

### DemoShapeSDF04
[source code](src/demo/kotlin/DemoShapeSDF04.kt)

![DemoShapeSDF04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF04Kt.png)

### DemoShapeSDF05
[source code](src/demo/kotlin/DemoShapeSDF05.kt)

![DemoShapeSDF05Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF05Kt.png)

### DemoSkeleton01
[source code](src/demo/kotlin/DemoSkeleton01.kt)

![DemoSkeleton01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoSkeleton01Kt.png)

### DemoStraightSkeleton01
[source code](src/demo/kotlin/DemoStraightSkeleton01.kt)

![DemoStraightSkeleton01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoStraightSkeleton01Kt.png)
