# orx-obj-loader

Simple loader for Wavefront .obj 3D mesh files.

##### Usage

Loading directly into a vertex buffer can be done through `loadOBJasVertexBuffer`.

```kotlin
val vertexBuffer = loadOBJasVertexBuffer("data/someObject.obj")
```

The loaded vertex buffer can be drawn like this:
```kotlin
drawer.vertexBuffer(vertexBuffer, DrawPrimitive.TRIANGLES)
```


<!-- __demos__ -->
## Demos
### DemoWireframe01
[source code](src/demo/kotlin/DemoWireframe01.kt)

![DemoWireframe01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoWireframe01Kt.png)
