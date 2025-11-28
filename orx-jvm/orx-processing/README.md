# orx-processing

orx-processing is a module designed to facilitate seamless type conversions
between Processing's types and OPENRNDR's types. It provides utilities and
methods that allow developers to integrate the two graphics frameworks
effectively by bridging the gap between their respective data structures.

For example, orx-processing enables you to:
 - Convert Processing's PVector to OPENRNDR's Vector2 or Vector3.
 - Transform OPENRNDR Shape and ShapeContour into their Processing equivalents.
 
This module is particularly useful in projects that require the features or
APIs of both Processing and OPENRNDR, simplifying interoperability and reducing boilerplate code for type translation.
<!-- __demos__ -->
## Demos
### DemoPShape01

Demonstrates how to construct a Processing `PShape` out of an OPENRNDR
`Shape` instance, and how to convert a `PShape` back into a `Shape.

The program renders a rectangular `Shape` after converting to PShape and back.


![DemoPShape01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-processing/images/DemoPShape01Kt.png)

[source code](src/demo/kotlin/DemoPShape01.kt)

### DemoPShape02

Demonstrates how to convert a `ShapeContour` into a Processing
`PShape`, then converts the `PShape` to a `Shape`.

The program renders both the original `ShapeContour` and
the resulting `Shape` after being a `PShape`.

Both elements are rendered with translucency and a slight offset
so they can be visually compared.

![DemoPShape02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-processing/images/DemoPShape02Kt.png)

[source code](src/demo/kotlin/DemoPShape02.kt)

### DemoPShape03

Demonstrates how to convert a `Shape` with multiple `ShapeContour`s
(an outer contour and two holes) into a Processing `PShape`,
then converts it back to a `Shape`.

The program renders both the original `Shape` and
the resulting `Shape` with translucency and a slight offset
so they can be visually compared.

![DemoPShape03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-processing/images/DemoPShape03Kt.png)

[source code](src/demo/kotlin/DemoPShape03.kt)
