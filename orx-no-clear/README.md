# orx-no-clear

A simple OPENRNDR Extension that provides the classical drawing-without-clearing-the-screen functionality that
OPENRNDR does not support natively.

#### Usage

```kotlin
fun main() = application {
    configure {
        title = "NoClearProgram"
    }
    program {
        backgroundColor = ColorRGBa.PINK
        extend(NoClear())
        extend {
            drawer.circle(Math.cos(seconds) * width / 2.0 + width / 2.0, Math.sin(seconds * 0.24) * height / 2.0 + height / 2.0, 20.0)
        }
    }
}
```

#### Usage with additional configuration
Optionally, a static `backdrop` may be setup by providing custom code.

- Example 1. Customising the backdrop with an image
```kotlin
extend(NoClear()) {
    val img = loadImage("data\\backdrop.png")
    backdrop = {
        drawer.image(img, 0.0, 0.0, width * 1.0, height * 1.0)
    }
}
```

- Example 2. Customising the backdrop with a checker-board pattern
```kotlin
extend(NoClear()) {
    backdrop = {
        val xw = width / 8.0
        val yh = height / 8.0
        drawer.fill = ColorRGBa.RED
        (0..7).forEach { row ->
            (0..7).forEach { col ->
                if ((row + col) % 2 == 0) {
                    drawer.rectangle(row * xw, col * yh, xw, yh)
                }
            }
        }
    }
}
```

NB! any submitted _lambda expression_ must be valid within the `renderTarget` context.