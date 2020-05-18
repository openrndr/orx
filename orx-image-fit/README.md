# orx-image-fit

Draws the given image making sure it fits (`contain`) or it covers (`cover`) the specified area.

Similar to CSS object-fit (https://www.w3schools.com/css/css3_object-fit.asp)

`orx-image-fit` provides an extension function `imageFit` for `Drawer`.

## Usage

`imageFit(img: ColorBuffer, x: Double, y: Double, w: Double, h: Double, fitMethod, horizontalPosition:Double, verticalPosition:Double)`

fitMethod
 - `contain`
 - `cover`
 
horizontal values
 - left ... right
 - `-1.0` ... `1.0`
 
 vertical values
 - top ... bottom
 - `-1.0` ... `1.0`
 
## Example 
 
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
 
