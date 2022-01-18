# orx-compositor

Toolkit to make composite (layered) images using blend modes and filters.

##### Usage

```kotlin
import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.edges.EdgesWork
import org.openrndr.extra.gui.GUI
import org.openrndr.math.Vector2


fun main() {
    application {
        configure {
            width = 768
            height = 768
        }
        program {
            val gui = GUI()

            val w2 = width / 2.0
            val h2 = height / 2.0

            val c = compose {
                draw {
                    drawer.fill = ColorRGBa.PINK
                    drawer.circle(width / 2.0, height / 2.0, 10.0)
                }
    
                layer {
                    blend(Add())

                    draw {
                        drawer.circle(width / 2.0, height / 2.0, 100.0)
                    }
                    post(ApproximateGaussianBlur()) {
                        window = 10
                        sigma = Math.cos(seconds * 10.0) * 10.0 + 10.0
                    }
                }
            }
            extend(gui)
            extend {
                c.draw(drawer)
            }
        }
    }
}
```
<!-- __demos__ -->
## Demos
### DemoUse01
[source code](src/demo/kotlin/DemoUse01.kt)

![DemoUse01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-compositor/images/DemoUse01Kt.png)

### DemoCompositor01
[source code](src/demo/kotlin/DemoCompositor01.kt)

![DemoCompositor01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-compositor/images/DemoCompositor01Kt.png)
