# orx-boofcv

An extension that simplifies the use of the BoofCV computer vision
library inside OPENRNDR.

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
