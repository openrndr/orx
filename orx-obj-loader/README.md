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
### DemoObjLoader01
[source code](src/demo/kotlin/DemoObjLoader01.kt)

![DemoObjLoader01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoObjLoader01Kt.png)

### DemoObjSaver01
[source code](src/demo/kotlin/DemoObjSaver01.kt)

![DemoObjSaver01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoObjSaver01Kt.png)

### DemoObjSaver02
[source code](src/demo/kotlin/DemoObjSaver02.kt)

![DemoObjSaver02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoObjSaver02Kt.png)
