# orx-no-clear

Provides the classical "draw-without-clearing-the-screen" functionality.

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
    val img = loadImage("data/backdrop.png")
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
<!-- __demos__ -->
## Demos
### DemoNoClear01

By default, OPENRNDR clears the canvas on each animation
frame. [NoClear] disables that behavior, letting you
draw on top of what you drew in previous animation frames.

That's the default in some other frameworks.

![DemoNoClear01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-no-clear/images/DemoNoClear01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoNoClear01.kt)

### DemoNoClear02

Demonstrates the `NoClear` extension with `FLOAT32` color type.

The program draws circles around the center using additive blending.
The fill colors are dark and accumulate to create a visible effect.

By default, the color type is `UINT8`, which provides 256 brightness levels
per color channel (red, green, blue).

With `UINT8`, very small color increments (like a HSV value below 1.0/256.0,
e.g., 0.0035 instead of 0.035) would be too small to register, and the brightness
would not increase. Using `FLOAT32` solves this by supporting a finer precision.

![DemoNoClear02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-no-clear/images/DemoNoClear02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoNoClear02.kt)
