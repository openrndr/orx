# orx-text-writer

Writing texts with layouts

The `TextWriter` class was previously called `Writer`.
Find usage examples [in the guide](https://guide.openrndr.org/drawing/text.html#advanced-text-rendering).

_The code in `orx-text-writer` was previously found under `openrndr-draw` in the `openrndr` repository._
<!-- __demos__ -->
## Demos
### DemoTextWriter01

This demo features the drawing of a centered rectangle and the addition of styled text inside
the rectangle. The application manages the drawing of shapes and implementation of text rendering
with specific font and settings.

The following operations are performed:
- A rectangle is created from the center of the drawing bounds.
- The rectangle is drawn without a fill and with a white stroke.
- A custom font is loaded and applied to the drawer.
- A `TextWriter` is utilized to display the text "hello world" inside the rectangle, adhering to
specific styling and formatting rules.

Key Components:
- `application` establishes the visual environment.
- `Rectangle` provides a way to define the rectangular area.
- `drawer` enables isolated operations for drawing elements.
- `writer` facilitates text rendering with alignment and spacing adjustments.

![DemoTextWriter01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoTextWriter01Kt.png)

[source code](src/jvmDemo/kotlin/DemoTextWriter01.kt)

### DemoTextWriter02



![DemoTextWriter02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoTextWriter02Kt.png)

[source code](src/jvmDemo/kotlin/DemoTextWriter02.kt)

### DemoTextWriter03



![DemoTextWriter03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoTextWriter03Kt.png)

[source code](src/jvmDemo/kotlin/DemoTextWriter03.kt)
