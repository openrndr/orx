# orx-mesh-generators

Simple mesh generators for OPENRNDR

##### usage

```kotlin
val sphere = sphereMesh(32, 32, 4.0)
val unitSphere = sphereMesh()
val cube = boxMesh()
val box = boxMesh(2.0, 4.0, 2.0)

...

drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
drawer.vertexBuffer(unitSphere, DrawPrimitive.TRIANGLES)
drawer.vertexBuffer(cube, DrawPrimitive.TRIANGLES)
drawer.vertexBuffer(box, DrawPrimitive.TRIANGLES)

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
<!-- __demos__ >
# Demos
[DemoBoxKt](src/demo/kotlin/DemoBoxKt.kt
![DemoBoxKt](https://github.com/openrndr/orx/blob/media/orx-mesh-generators/images/DemoBoxKt.png
[DemoComplex01Kt](src/demo/kotlin/DemoComplex01Kt.kt
![DemoComplex01Kt](https://github.com/openrndr/orx/blob/media/orx-mesh-generators/images/DemoComplex01Kt.png
[DemoComplex02Kt](src/demo/kotlin/DemoComplex02Kt.kt
![DemoComplex02Kt](https://github.com/openrndr/orx/blob/media/orx-mesh-generators/images/DemoComplex02Kt.png
[DemoComplex03Kt](src/demo/kotlin/DemoComplex03Kt.kt
![DemoComplex03Kt](https://github.com/openrndr/orx/blob/media/orx-mesh-generators/images/DemoComplex03Kt.png
[DemoComplex04Kt](src/demo/kotlin/DemoComplex04Kt.kt
![DemoComplex04Kt](https://github.com/openrndr/orx/blob/media/orx-mesh-generators/images/DemoComplex04Kt.png
[DemoComplex05Kt](src/demo/kotlin/DemoComplex05Kt.kt
![DemoComplex05Kt](https://github.com/openrndr/orx/blob/media/orx-mesh-generators/images/DemoComplex05Kt.png
<!-- __demos__ -->
## Demos
### DemoBox
[source code](src/demo/kotlin/DemoBox.kt)

![DemoBoxKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoBoxKt.png)

### DemoComplex01
[source code](src/demo/kotlin/DemoComplex01.kt)

![DemoComplex01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex01Kt.png)

### DemoComplex02
[source code](src/demo/kotlin/DemoComplex02.kt)

![DemoComplex02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex02Kt.png)

### DemoComplex03
[source code](src/demo/kotlin/DemoComplex03.kt)

![DemoComplex03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex03Kt.png)

### DemoComplex04
[source code](src/demo/kotlin/DemoComplex04.kt)

![DemoComplex04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex04Kt.png)

### DemoComplex05
[source code](src/demo/kotlin/DemoComplex05.kt)

![DemoComplex05Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/DemoComplex05Kt.png)
