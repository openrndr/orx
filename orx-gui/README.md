# orx-gui

A quick-and-dirty user interface toolkit.

`orx-gui` uses class and property annotations to generate simple interfaces. The annotations used 
are provided by [`orx-parameters`](../orx-parameters/README.md) and most filters in [`orx-fx`](../orx-fx/README.md) have been annotated.

`orx-gui` is made with an [`orx-olive`](../orx-olive/README.md) workflow in mind but can be used in normal OPENRNDR programs
just as well.

## Usage

Preparation: make sure `orx-gui` is in the `orxFeatures` of your project (if you working on a template based project)

The essence of `orx-gui` lies in the provided a `GUI` extension, which can be used in your program using the `extend {}` function. 
The `GUI` class has an `add()` function that allows any annotated object to be passed in.

The visibility of the side bar can be toggled by pressing the F11 key on your keyboard.

### UIs for parameter objects

A simple UI can be created by creating an annotated `object`.

```kotlin
import org.openrndr.application
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

fun main() = application {
    program {
        // -- this @Description annotation is optional
        val parameters = @Description("parameters") object {
            @DoubleParameter("x value", 0.0, 640.0)
            var x = 0.5

            @DoubleParameter("y value", 0.0, 480.0)
            var y = 0.5
        }

        extend(GUI()) {
            add(parameters)
        }
        extend {
            drawer.circle(parameters.x, parameters.y, 200.0)
        }
    }
}
```

### UIs for filters

In a similar fashion to the previous example we can create a simple UI for most filters in `orx-fx`

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.gui.GUI

fun main() = application {
    program {
        val blur = BoxBlur()
        val rt = renderTarget(width, height) {
            colorBuffer()
        }
        extend(GUI()) {
            add(blur)
        }
        extend {
            drawer.isolatedWithTarget(rt) {
                drawer.background(ColorRGBa.BLACK)
                drawer.fill = ColorRGBa.PINK
                drawer.circle(width / 2.0, height / 2.0, 200.0)
            }
            blur.apply(rt.colorBuffer(0), rt.colorBuffer(0))
            drawer.image(rt.colorBuffer(0))
        }
    }
}
```

### UIs in Olive

Using `orx-gui` in Olive (`orx-olive`) is very similar to how one would use it in a normal OPENRNDR program. There is
one detail that doesn't occur in normal programs: the UI state is reset when a
script is changed and re-evaluated. This is overcome by using an annotated `Reloadable` object.

An example `live.kts` script that uses `orx-gui` and `Reloadable`:
```kotlin
@file:Suppress("UNUSED_LAMBDA_EXPRESSION")
import org.openrndr.Program
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.Reloadable
import org.openrndr.extra.parameters.DoubleParameter

{ program: Program ->
    program.apply {
        val p = object : Reloadable() {
            @DoubleParameter("x-position", 0.0,  640.0, order = 0)
            var x = 0.5
            @DoubleParameter("y-position", 0.0, 480.0, order = 1)
            var y = 0.5
            @DoubleParameter("radius", 0.0, 480.0, order = 2)
            var radius = 100.0
        }
        p.reload()

        extend(GUI()) {
            add(p)
        }
        extend {
            drawer.circle(p.x, p.y, p.radius)
        }
    }
}
```
