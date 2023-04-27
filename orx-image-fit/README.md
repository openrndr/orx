# orx-image-fit

Draws an image ensuring it fits or covers the specified `Rectangle`.

Similar to CSS object-fit (https://developer.mozilla.org/en-US/docs/Web/CSS/object-fit)

`orx-image-fit` provides the `Drawer.imageFit` extension function.

## Usage

```
drawer.imageFit(
  img: ColorBuffer, 
  x: Double, y: Double, w: Double, h: Double, 
  horizontalPosition: Double, 
  verticalPosition: Double,
  fitMethod: FitMethod)
```

or 

```
drawer.imageFit(
  img: ColorBuffer, 
  bounds: Rectangle, 
  horizontalPosition: Double, 
  verticalPosition: Double,
  fitMethod: FitMethod)
```

- `img`: the image to draw 
- `x`, `y`, `w`, `h` or `bounds`: the target area where to draw the image
- `fitMethod`: 
  - `FitMethod.Contain`: fits `img` in the target area. If the aspect ratio of `img` and `bounds` differ it leaves blank horizontal or vertical margins to avoid deforming the image.
  - `FitMethod.Cover`: covers the target area. . If the aspect ratio of `img` and `bounds` differ part of the image will be cropped away.
  - `FitMethod.Fill`: deforms the image to exactly match the target area.
  - `FitMethod.None`: draws the image on the target area without scaling it.
- `horizontalPosition` and `verticalPosition`: controls which part of the image is visible (`Cover`, `None`) or the alignment of the image (`Contain`). 
  - `horizontalPosition`: `-1.0` = left, `0.0` = center, `1.0` = right.
  - `verticalPosition`: `-1.0` = top, `0.0` = center, `1.0` = bottom.

## Examples
 
A quick example that fits an image to the window rectangle with a 10 pixel margin. By default
`imageFit` uses the cover mode, which fills the target rectangle with an image.
  
```kotlin
fun main() = application {
    program {
        val image = loadImage("data/images/pm5544.png")
        extend {
            drawer.imageFit(image, 10.0, 10.0, width - 20.0, height - 20.0)
        }
    }
}
``` 

or

```kotlin
fun main() = application {
    program {
        val image = loadImage("data/images/pm5544.png")
        extend {
            drawer.imageFit(image, drawer.bounds.offsetEdges(-10.0))
        }
    }
}
``` 
<!-- __demos__ -->
## Demos
### DemoImageFit01
[source code](src/jvmDemo/kotlin/DemoImageFit01.kt)

![DemoImageFit01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-image-fit/images/DemoImageFit01Kt.png)
