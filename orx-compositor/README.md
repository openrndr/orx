# orx-compositor

A simple toolkit to make composite images.

##### Usage

```kotlin
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.*
import org.openrndr.filter.blend.add
import org.openrndr.filter.blur.ApproximateGaussianBlur

class Compositor001 : Program() {

    override fun setup() {
        val drawing = compose {
            draw {
                drawer.fill = ColorRGBa.PINK
                drawer.circle(width / 2.0, height / 2.0, 10.0)
            }

            layer {
                draw {
                    drawer.circle(width / 2.0, height / 2.0, 100.0)
                }
                post(ApproximateGaussianBlur()) {
                    window = 10
                    sigma = Math.cos(seconds * 10.0) * 10.0 + 10.0
                }
                blend(add)
            }
        }

        extend {
            drawing.draw(drawer)
        }
    }
}

fun main(args: Array<String>) = application(Compositor001(), Configuration())
```
