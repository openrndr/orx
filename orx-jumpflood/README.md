# orx-jumpflood

Calculates distance or direction fields from an image.
GPU accelerated, 2D. Results are provided as an image.

[Original jump flooding algorithm](https://www.comp.nus.edu.sg/~tants/jfa.html)

`orx-jumpflood` focusses on finding 2d distance and directional distance fields.

## Distance field example

`distanceFieldFromBitmap()` calculates distances to bitmap contours it stores
the distance in red and the original bitmap in green.


```kotlin
import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.jumpfill.DistanceField
import org.openrndr.extra.jumpfill.Threshold
import org.openrndr.ffmpeg.VideoPlayerFFMPEG

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

        val distanceField = DistanceField()
        val distanceFieldBuffer = colorBuffer(width, height, type = ColorType.FLOAT32)

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

            distanceField.apply(thresholded, distanceFieldBuffer)

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
                drawer.image(distanceFieldBuffer)
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
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.jumpfill.DirectionalField
import org.openrndr.extra.jumpfill.Threshold
import org.openrndr.ffmpeg.VideoPlayerFFMPEG

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

        val directionField = DirectionalField()
        val directionalFieldBuffer = colorBuffer(width, height, type = ColorType.FLOAT32)

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

            directionField.apply(thresholded, directionalFieldBuffer)

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
                drawer.image(directionalFieldBuffer)
            }
        }
    }
}
```
<!-- __demos__ -->
## Demos
### DemoDirectionField01

Shows how to use the [DirectionalField] filter.
Draws moving white shapes on black background,
then applies the DirectionalField filter which returns a [ColorBuffer] in which
the red and green components encode the direction to the closest black/white edge,
and the blue component the distance to that edge.

Hold down a mouse button to see the raw animation.

![DemoDirectionField01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoDirectionField01Kt.png)

[source code](src/jvmDemo/kotlin/DemoDirectionField01.kt)

### DemoDirectionField02

Demonstrates how use the `DirectionalField` effect to create
a `ColorBuffer` in which the RGB components encode direction and distance to the closest
edge of every pixel in an input `ColorBuffer`.

The program draws scattered white circles on a `ColorBuffer`, then applies the `DistanceField()`
effect and renders the static result on every animation frame.

Additionally, it uses the shadow (CPU version of the texture) to query the distance field texture
at current mouse position. The resulting blue color component is used as the radius of a circle
centered at the mouse position. The red and green components are used to draw a line to the
black/white edge closest to the mouse pointer.

![DemoDirectionField02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoDirectionField02Kt.png)

[source code](src/jvmDemo/kotlin/DemoDirectionField02.kt)

### DemoDistanceField01

Shows how to use the [DistanceField] filter.

Draws moving white shapes on black background,
then applies the DistanceField filter which returns a [ColorBuffer] in which
the red component encodes the distance to the closest black/white edge.

The value of the green component is negative when on the black background
and positive when inside white shapes. The sign is used in the [shadeStyle] to choose
between two colors.

The inverse of the distance is used to obtain a non-linear brightness.

Hold down a mouse button to see the raw animation.

![DemoDistanceField01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoDistanceField01Kt.png)

[source code](src/jvmDemo/kotlin/DemoDistanceField01.kt)

### DemoShapeSDF01

Demonstrates the use of the `ShapeSDF()` effect, which takes vector shapes
(either `Shape` or `ShapeContour` instances) and produces a `ColorBuffer`
texture containing a signed distance field pointing at the closest vector edge
encoded in its RGB channels.

Hold down any mouse button to observe the original vector shape in black and white,
without the effect applied.

![DemoShapeSDF01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF01Kt.png)

[source code](src/jvmDemo/kotlin/DemoShapeSDF01.kt)

### DemoShapeSDF02

Advanced demonstration making use of two `ShapeSDF` filters: the first applied to a static
SVG loaded from a file, and the second to a rotating version of the same shape.

The demo also uses three additional SDF filters:
- `SDFSmoothIntersection`, which combines two color buffers containing SDF information
into a new color buffer
- `SDFOnion`, which takes one SDF color buffer and outputs one SDF color buffer
- `SDFStrokeFill`, used for rendering using configurable fill and stroke properties.

The vertical mouse position is used to control the radius of the `SDFSmoothIntersection` effect.

The program finally renders the result of the previous operations as one color buffer
thanks to the `SDFStrokeFill` effect.

![DemoShapeSDF02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF02Kt.png)

[source code](src/jvmDemo/kotlin/DemoShapeSDF02.kt)

### DemoShapeSDF03

Advanced demonstration making use the `ShapeSDF` filter applied twice to a static
SVG loaded from a file, one with `useUV` set to true.

A `FluidDistort` filter is used to generate an animated UV map which is fed into
both `ShapeSDF` filters. A `SDFSmoothDifference` filter is then applied to combine
both resulting `ColorBuffer` instances, and a `SDFStrokeFill` filter used for
rendering the result.

The mouse horizontal position determines which of the three used color buffers is
displayed.

![DemoShapeSDF03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF03Kt.png)

[source code](src/jvmDemo/kotlin/DemoShapeSDF03.kt)

### DemoShapeSDF04

Demonstrates using tow `ShapeSDF` filters. One contairs a vector shape loaded
from disk, the other a circular shape.

A `Perturb` effect is used to generate a noise UV map, which is then fed into
the `ShapeSDF` filters.

The two resulting `ColorBuffer`s are combined using a `SDFSmoothDifference` filter,
which erases the distorted circular shape from the loaded shape.

The `SDFStrokeFill` is used to render the result.

A GUI is available to tweak the parameters of the `Perturb` effect.
Lowering its `gain` to zero disables the effect, revealing the circle and the
smoothness (round corners) of the difference effect.

![DemoShapeSDF04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF04Kt.png)

[source code](src/jvmDemo/kotlin/DemoShapeSDF04.kt)

### DemoShapeSDF05

Variation of DemoShapeSDF04, in which `Perturb` is applied twice with different
parameters for a more complex UV map, and with four effects added to the GUI
for further customization and exploration.

![DemoShapeSDF05Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoShapeSDF05Kt.png)

[source code](src/jvmDemo/kotlin/DemoShapeSDF05.kt)

### DemoSkeleton01



![DemoSkeleton01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoSkeleton01Kt.png)

[source code](src/jvmDemo/kotlin/DemoSkeleton01.kt)

### DemoStraightSkeleton01



![DemoStraightSkeleton01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoStraightSkeleton01Kt.png)

[source code](src/jvmDemo/kotlin/DemoStraightSkeleton01.kt)

### DemoVoronoi01



![DemoVoronoi01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoVoronoi01Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi01.kt)

### DemoVoronoi02



![DemoVoronoi02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoVoronoi02Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi02.kt)

### DemoVoronoi03



![DemoVoronoi03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jumpflood/images/DemoVoronoi03Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi03.kt)
