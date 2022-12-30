# orx-g-code

Utilities for generating *g-code* from compositions. 

>*Make sure to verify compatibility to your hardware and that the
commands do not exceed the machines limits before running any output of this.*

## Generator

A [Generator](src/commonMain/kotlin/Generator.kt) generates g-code for the following operations:
- `setup`: Setup code at the beginning of a file.
- `moveTo`: A move operation to the given location.
- `preDraw`: Start drawing sequence. Pen down, laser on, etc.
- `drawTo`: A draw operation to the given location.
- `postDraw`: End draw sequence. Lift pen, turn laser off, etc.
- `end`: End of file sequence.
- `comment`: Insert a comment.

These are used by the `toCommands()` extensions in [shape.kt](src/commonMain/kotlin/shape.kt) to convert a composition
to a set of Commands. 

It only supports absolute moves, so `G90 absolute positioning` should be included in the setup.

`basicGrblSetup` defines an example that outputs g-code, which *should be*
compatible with [grbl v1.1](https://github.com/grbl/grbl).

See [DemoGcodeGenerator.kt](src/demo/kotlin/DemoGcodeGenerator.kt) for an example of how to use.

## Plot (jvm only)

The [Plot Extension](src/jvmMain/kotlin/Plot.kt) provides a quick setup for drawing, rendering and exporting files for
a pen plotter. 
See [DemoSimplePlot.kt](src/demo/kotlin/DemoSimplePlot.kt) for an example of how to use.


<!-- __demos__ -->
## Demos
### DemoInteractivePlot
[source code](src/demo/kotlin/DemoInteractivePlot.kt)

![DemoInteractivePlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-g-code/images/DemoInteractivePlotKt.png)

### DemoSimplePlot
[source code](src/demo/kotlin/DemoSimplePlot.kt)

![DemoSimplePlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-g-code/images/DemoSimplePlotKt.png)
