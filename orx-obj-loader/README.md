# orx-obj-loader

A simple loader for Wavefront .obj files

##### Usage

Loading directly into a vertex buffer can be done through `loadOBJasVertexBuffer`.

```kotlin
val vertexBuffer = loadOBJasVertexBuffer("data/someObject.obj")
```

The loaded vertex buffer can be drawn like this:
```kotlin
drawer.vertexBuffer(vertexBuffer, DrawPrimitive.TRIANGLES)
```


