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



![DemoInteractivePlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-g-code/images/DemoInteractivePlotKt.png)

[source code](src/jvmDemo/kotlin/DemoInteractivePlot.kt)

### DemoSimplePlot



![DemoSimplePlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-g-code/images/DemoSimplePlotKt.png)

[source code](src/jvmDemo/kotlin/DemoSimplePlot.kt)
