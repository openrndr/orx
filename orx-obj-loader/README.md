# orx-obj-loader

Simple loader and saver for Wavefront .obj 3D mesh files.

##### Usage

Loading directly into a vertex buffer can be done through `loadOBJasVertexBuffer`.

```kotlin
val vertexBuffer = loadOBJasVertexBuffer("data/someObject.obj")
```

The loaded vertex buffer can be drawn like this:

```kotlin
drawer.vertexBuffer(vertexBuffer, DrawPrimitive.TRIANGLES)
```

To save a vertex buffer as an .obj file:

```kotlin
vertexBuffer.saveOBJ("my/path/exported.obj")
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

### DemoWireframe01
[source code](src/demo/kotlin/DemoWireframe01.kt)

![DemoWireframe01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoWireframe01Kt.png)
