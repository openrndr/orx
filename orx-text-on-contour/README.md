# orx-text-on-contour

Write texts on contours.

<!-- __demos__ -->
## Demos
### DemoSlugTextOnContour01

Demo Functionality includes:
- Loading and applying a specific font (`IBMPlexMono-Regular`) with a size of 32.0.
- Creating a circular contour at the center of the screen with a radius of 200.0.
- Rendering text along the rectified circle's contour.
- Offsetting text positions, enabling repeated text rendering along the same contour.

![DemoSlugTextOnContour01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-on-contour/images/DemoSlugTextOnContour01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoSlugTextOnContour01.kt)

### DemoSlugTextOnContour02

Demo Functionality includes:
- Placing text on a circular contour with different baseline offsets.
- A positive offset moves the text away from the contour towards its ascenders, a negative offset moves it the
other way. Glyph spacing is measured on the offset baseline, so the text keeps its natural spacing.
- A superscript placed with [TextStyle.baselineShiftInEm].

![DemoSlugTextOnContour02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-on-contour/images/DemoSlugTextOnContour02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoSlugTextOnContour02.kt)

### DemoSlugTextOnContours01

Demo Functionality includes:
- Loading and applying a specific font (`IBMPlexMono-Regular`).
- Creating a series of concentric circular contours.
- Typesetting a text such that it overflows from one contour to the next.

![DemoSlugTextOnContours01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-on-contour/images/DemoSlugTextOnContours01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoSlugTextOnContours01.kt)

### DemoSlugTextOnContours02

Demo Functionality includes:
- Typesetting right-to-left Arabic text (Article 1 of the Universal Declaration of Human Rights)
such that it overflows from one wavy contour to the next.
- Using [Direction.RIGHT_TO_LEFT] to lay out the words of every line from right to left.

![DemoSlugTextOnContours02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-on-contour/images/DemoSlugTextOnContours02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoSlugTextOnContours02.kt)

### DemoTextOnContour01

Demo Functionality includes:
- Loading and applying a specific font (`IBMPlexMono-Regular`) with a size of 32.0.
- Creating a circular contour at the center of the screen with a radius of 200.0.
- Rendering text along the rectified circle's contour.
- Offsetting text positions, enabling repeated text rendering along the same contour.

![DemoTextOnContour01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-text-on-contour/images/DemoTextOnContour01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoTextOnContour01.kt)
