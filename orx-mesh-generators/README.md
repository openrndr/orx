# orx-mesh-generators

Generates various types of 3D meshes.

## Simple usage

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

`orx-mesh-generators` comes with `buildTriangleMesh` 

```kotlin
buildTriangleMesh {
    rotate(Vector3.UNIT_Z, 45.0)
    box()
}

```

## API

```kotlin
fun sphereMesh(
    sides: Int = 16,
    segments: Int = 16,
    radius: Double = 1.0,
    invert: Boolean = false): VertexBuffer

fun groundPlaneMesh(
    width: Double = 1.0,
    height: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int): VertexBuffer

fun boxMesh(
    width: Double = 1.0,
    height: Double = 1.0,
    depth: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
    depthSegments: Int = 1,
    invert: Boolean = false): VertexBuffer
```

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
