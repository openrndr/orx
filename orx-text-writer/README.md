# orx-text-writer

Write texts with layouts.

The `TextWriter` class was previously called `Writer`.
Find usage examples [in the guide](https://guide.openrndr.org/drawing/text.html#advanced-text-rendering).

_The code in `orx-text-writer` was previously found under `openrndr-draw` in the `openrndr` repository._
<!-- __demos__ -->
## Demos
- [DemoGlyphOutput01](#demoglyphoutput01)
- [DemoTextBreaker01](#demotextbreaker01)
- DemoTextWriter [01](#demotextwriter01), [02](#demotextwriter02), [03](#demotextwriter03)
- **slug**
  - DemoSlugTextDrawer [01](#slugdemoslugtextdrawer01), [02](#slugdemoslugtextdrawer02), [03](#slugdemoslugtextdrawer03), [04](#slugdemoslugtextdrawer04), [05](#slugdemoslugtextdrawer05), [06](#slugdemoslugtextdrawer06), [07](#slugdemoslugtextdrawer07), [08](#slugdemoslugtextdrawer08), [09](#slugdemoslugtextdrawer09)
  - [DemoVectorTextDrawer01](#slugdemovectortextdrawer01)

### DemoGlyphOutput01

This demo implements a drawing program utilizing custom text rendering with a wave-like animation effect.
It allows for manipulating text position and scaling over time.

Key elements of the program:
- A centered rectangle on the drawing canvas.
- Text rendering with properties such as horizontal alignment, vertical alignment, and tracking,
dynamically changing over time.
- Custom text animation implementing wave-like movement and scaling.

![DemoGlyphOutput01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoGlyphOutput01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoGlyphOutput01.kt)

### DemoTextBreaker01

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

![DemoTextBreaker01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoTextBreaker01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoTextBreaker01.kt)

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

![DemoTextWriter01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoTextWriter01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoTextWriter01.kt)

### DemoTextWriter02

Demonstrates the `writer`s `style.horizontalAlign` property,
which enables left (0.0), center (0.5), right (1.0) text alignment
and any values in between.

The program creates a 3x3 grid of texts and interpolates their alignments
between left and right using the cosine of the current time in seconds.

A time offset is included in each cell to distribute them over the
cosine wave, so the text lines move at different speeds and directions.

![DemoTextWriter02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoTextWriter02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoTextWriter02.kt)

### DemoTextWriter03

This demo shows how to align texts to the left, center, right, top, center and bottom of a container box.

It creates a grid of 3x3 cells to demonstrate all alignment combinations by setting the
`style.verticalAlign` and the `style.horizontalAlign` to 0.0, 0.5 and 1.0.

![DemoTextWriter03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/DemoTextWriter03Kt.webp)

[source code](src/jvmDemo/kotlin/DemoTextWriter03.kt)

### slug/DemoSlugTextDrawer01



![slug-DemoSlugTextDrawer01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer01Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer01.kt)

### slug/DemoSlugTextDrawer02



![slug-DemoSlugTextDrawer02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer02Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer02.kt)

### slug/DemoSlugTextDrawer03



![slug-DemoSlugTextDrawer03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer03Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer03.kt)

### slug/DemoSlugTextDrawer04



![slug-DemoSlugTextDrawer04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer04Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer04.kt)

### slug/DemoSlugTextDrawer05

Demonstrates typesetting Arabic script: contextual letter forms, ligatures and right-to-left lines.
The text is Article 1 of the Universal Declaration of Human Rights.

![slug-DemoSlugTextDrawer05Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer05Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer05.kt)

### slug/DemoSlugTextDrawer06

Demonstrates bidirectional text: a left-to-right paragraph with embedded Arabic words and a
right-to-left paragraph with embedded English words and numbers.

Every embedded run keeps its own reading order, multi-word runs are not reversed word by word.

![slug-DemoSlugTextDrawer06Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer06Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer06.kt)

### slug/DemoSlugTextDrawer07

Demonstrates hyphenation: the same text in two justified columns, on the left without hyphenation and on the right
hyphenated automatically by setting [TextStyle.language].

![slug-DemoSlugTextDrawer07Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer07Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer07.kt)

### slug/DemoSlugTextDrawer08

Demonstrates text flowing into shapes: justified and hyphenated text fills a ring, lines next to the hole are split
in two, and the text continues in a rectangle with a circular cut-out.

![slug-DemoSlugTextDrawer08Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer08Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer08.kt)

### slug/DemoSlugTextDrawer09

Demonstrates vertical alignment of text in shapes: the same text in three circles, aligned to the top, the center
and the bottom.

![slug-DemoSlugTextDrawer09Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoSlugTextDrawer09Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoSlugTextDrawer09.kt)

### slug/DemoVectorTextDrawer01



![slug-DemoVectorTextDrawer01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-writer/images/slug-DemoVectorTextDrawer01Kt.webp)

[source code](src/jvmDemo/kotlin/slug/DemoVectorTextDrawer01.kt)
