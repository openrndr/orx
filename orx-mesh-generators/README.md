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
