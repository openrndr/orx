# orx-g-code

Utilities for generating g-code for CNC Machines such as pen plotters, laser engravers, 3D printers, and more.

**Features:**
- Generate g-code from compositions.
- Set up and preview a drawer with a document coordinate space.
- Customizable g-code generation to fit your hardware.

> *Make sure to verify compatibility to your hardware and that the
commands do not exceed the machine limits before running any output.*

## Plot (jvm only)

The [Plot Extension](src/jvmMain/kotlin/Plot.kt) provides a quick setup for drawing, rendering and exporting files for
a pen plotter.
See [DemoSimplePlot.kt](src/jvmDemo/kotlin/DemoSimplePlot.kt) for an example of how to use it.

### Usage
```kotlin
fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        extend(Plot(dimensions = Vector2(210.0, 297.0))) {
            generator = BasicGrblGenerator()
            draw {
                circle(docBounds.center, 21.0)
            }
        }
    }
}
```

## Generator

A [Generator](src/commonMain/kotlin/generator.kt) provides the context for traversing a composition, shapes and contours
to render it to g-code.

The methods of the [GeneratorContexet](src/commonMain/generator.kt#L31) are called by the `render()` extensions
in [render.kt](src/commonMain/kotlin/extensions/shape.kt) to convert a composition
to a set of Commands within the context.

Each hook (`beginFile`, `beginLayer`, `beginShape`, `beginContour`, `drawTo`, `endContour`, `endShape`, `endLayer`,
`endFile`) should produce appropriate commands for that event for the target machine.

For example, the `beginContour` hook could move to start of the contour and enable the drawing tool (pen down, laser on,
etc.).

### BasicGrblGenerator

The `BasicGrblGeneratorContext` is a reference implemenation of a `GeneratorContext` that outputs g-code that should be compatible
with [grbl](https://github.com/grbl/grbl).

It is created and configured using the `BaseGeneratorContext` class, which is a good starting point for implementing
a custom generator.

See [DemoGcodeGenerator.kt](src/jvmDemo/kotlin/DemoGcodeGenerator.kt) for an example of how to use it.

### Custom Generator and GeneratorContext

See [DemoCustomGcodeGenerator.kt](src/jvmDemo/kotlin/DemoCustomGcodeGenerator.kt) on how to implement a custom generator
tailored to your hardware or g-code flavor.

<!-- __demos__ -->
## Demos
### DemoInteractivePlot

This demo shows how to use the [Plot] class to draw using user input and render the result to G-code.

You can use the mouse drag to draw contours on the plot.

The input handling code shows how to convert mouse coordinates from the screen space to the document space.

Pressing the `g` key will render the g-code and write it to `/tmp`.

![DemoInteractivePlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-g-code/images/DemoInteractivePlotKt.png)

[source code](src/jvmDemo/kotlin/DemoInteractivePlot.kt)

### DemoSimplePlot

A minimal example of how to use the [Plot].

The Plot is set up to A4 Portrait paper, to generate grbl compatible g-code, to write each layer to a separate file
and to export the g-code to `/tmp`.

The default layer can be drawn to with the `draw` block.
Additional named layers can be created with `layer` block.

The application window shows a preview of the plot.
In this case a black rectrangle 1cm from the paper edges and 9 circles with radii from 10mm to 90mm.

Pressing `g` will write two files to the `/tmp` directory.
Note that setting the stroke will not affect the generated g-code.
It could be a hint to what pen color is used to draw each layer.

This does not use the olive orx to keep the example minimal. But using a program with live reloading,
you can quickly preview your plot.

![DemoSimplePlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-g-code/images/DemoSimplePlotKt.png)

[source code](src/jvmDemo/kotlin/DemoSimplePlot.kt)
