# orx-boofcv

Helper functions to ease working with the BoofCV computer vision library
and its data types.

BoofCV is an open source library written from scratch for real-time
computer vision. Its functionality covers a range of subjects,
low-level image processing, camera calibration, feature detection/tracking,
structure-from-motion, fiducial detection, and recognition.
BoofCV has been released under an Apache 2.0 license for both
academic and commercial use.

Examples of what BoofCV offers can be found at
[http://boofcv.org/](http://boofcv.org/)

As BoofCV implements it's own data types for images, lines, points, etc.
this addon provides some helper functions to convert them to OPENRNDR types:

- Bindings: converts to and from `ColorBuffer`.
- Drawing: allows directly drawing BoofCV line segments and other shapes.
- Point conversion to and from `Vector2`.
- Contour conversion from `BoofCV.Contour` to `Shape` and `ShapeContour`.
- `ImageFlow` to `ColorBuffer` conversion.

<!-- __demos__ -->
## Demos
### DemoContours01



![DemoContours01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-boofcv/images/DemoContours01Kt.png)

[source code](src/demo/kotlin/DemoContours01.kt)

### DemoResize01



![DemoResize01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-boofcv/images/DemoResize01Kt.png)

[source code](src/demo/kotlin/DemoResize01.kt)

### DemoResize02



![DemoResize02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-boofcv/images/DemoResize02Kt.png)

[source code](src/demo/kotlin/DemoResize02.kt)

### DemoSimplified01



![DemoSimplified01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-boofcv/images/DemoSimplified01Kt.png)

[source code](src/demo/kotlin/DemoSimplified01.kt)
