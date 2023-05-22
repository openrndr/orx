# orx-mesh-generators

3D-mesh generating functions and DSL.

## Simple meshes

```kotlin
// To create simple meshes
val sphere = sphereMesh(32, 32, 4.0)
val box = boxMesh(2.0, 4.0, 2.0)
val cylinder = cylinderMesh(radius = 0.5, length = 1.0, center = true)
val dodecahedron = dodecahedronMesh(0.5)
val plane = planeMesh(Vector3.ZERO, Vector3.UNIT_X, Vector3.UNIT_Y)
val disk = capMesh(sides = 15, radius = 0.5)
val tube = revolveMesh(sides = 15, length = 1.0)

// To draw the generated meshes
drawer.vertexBuffer(dodecahedron, DrawPrimitive.TRIANGLES)
```

## Complex triangular mesh generation

`orx-mesh-generators` comes with `buildTriangleMesh`, which
implements a [DSL](https://en.wikipedia.org/wiki/Domain-specific_language) 
to construct 3D shapes.

To create shapes we can call methods like `box()`, `sphere()`,
`cylinder()`, `dodecahedron()`, `plane()`, `revolve()`,
`taperedCylinder()`, `hemisphere()` and `cap()`.

```kotlin
// Create a rotated box
val mesh = buildTriangleMesh {
    rotate(Vector3.UNIT_Z, 45.0)
    box()
}
```

We can also use methods like `translate()` and `rotate()` to create
more complex compositions. The `color` property sets the color of
the next mesh.

```kotlin
// Create a ring of boxes of various colors
val mesh = buildTriangleMesh {
    repeat(12) {
        // Take a small step
        translate(2.0, 0.0, 0.0)
        // Turn 30 degrees
        rotate(Vector3.UNIT_Y, 30.0)
        // Set a color
        color = rgb(it / 11.0, 1.0, 1.0 - it / 11.0)
        // Add a colored box
        box(1.0, 1.0, 1.0)
    }
}
```

`isolated { ... }` can be used to encapsulate transformations and
avoid them accumulating to unpredictable values.

```kotlin
val mesh = buildTriangleMesh {
    repeat(10) { x ->
        repeat(10) { y ->
            isolated {
                translate(x * 1.0, y * 1.0, 0.0)
                sphere(8, 8, 0.1)
            }
        }
    }
}
```

Other available methods are:

- `grid()`: creates a tri-dimensional grid of meshes.
- `extrudeShape()`: gives depth to 2D `Shape`.
- `twist()`: post-processing effect to twist a mesh around an axis. 
- `extrudeContourSteps()`: uses Parallel Transport Frames to extrude a contour along a 3D path. 

The [demo folder](src/jvmDemo/kotlin) contains examples using these methods.

Check out the [source code](src/commonMain/kotlin) to learn about function arguments.

<!-- __demos__ -->
## Demos
### DemoAll
[source code](src/jvmDemo/kotlin/DemoAll.kt)

![DemoAllKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoAllKt.png)

### DemoBox
[source code](src/jvmDemo/kotlin/DemoBox.kt)

![DemoBoxKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoBoxKt.png)

### DemoComplex01
[source code](src/jvmDemo/kotlin/DemoComplex01.kt)

![DemoComplex01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex01Kt.png)

### DemoComplex02
[source code](src/jvmDemo/kotlin/DemoComplex02.kt)

![DemoComplex02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex02Kt.png)

### DemoComplex03
[source code](src/jvmDemo/kotlin/DemoComplex03.kt)

![DemoComplex03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex03Kt.png)

### DemoComplex04
[source code](src/jvmDemo/kotlin/DemoComplex04.kt)

![DemoComplex04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex04Kt.png)

### DemoComplex05
[source code](src/jvmDemo/kotlin/DemoComplex05.kt)

![DemoComplex05Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex05Kt.png)

### DemoComplex06
[source code](src/jvmDemo/kotlin/DemoComplex06.kt)

![DemoComplex06Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex06Kt.png)

### DemoExtrude01
[source code](src/jvmDemo/kotlin/DemoExtrude01.kt)

![DemoExtrude01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoExtrude01Kt.png)

### DemoExtrude02
[source code](src/jvmDemo/kotlin/DemoExtrude02.kt)

![DemoExtrude02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoExtrude02Kt.png)

### DemoExtrude03
[source code](src/jvmDemo/kotlin/DemoExtrude03.kt)

![DemoExtrude03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoExtrude03Kt.png)

### DemoExtrude04
[source code](src/jvmDemo/kotlin/DemoExtrude04.kt)

![DemoExtrude04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoExtrude04Kt.png)
