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
### DemoObjCompoundRW01



![DemoObjCompoundRW01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoObjCompoundRW01Kt.png)

[source code](src/jvmDemo/kotlin/DemoObjCompoundRW01.kt)

### DemoObjLoader01



![DemoObjLoader01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoObjLoader01Kt.png)

[source code](src/jvmDemo/kotlin/DemoObjLoader01.kt)

### DemoObjSaver01



![DemoObjSaver01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoObjSaver01Kt.png)

[source code](src/jvmDemo/kotlin/DemoObjSaver01.kt)

### DemoObjSaver02



![DemoObjSaver02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoObjSaver02Kt.png)

[source code](src/jvmDemo/kotlin/DemoObjSaver02.kt)

### DemoWireframe01

Display wireframe and non-planar faces

![DemoWireframe01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-obj-loader/images/DemoWireframe01Kt.png)

[source code](src/jvmDemo/kotlin/DemoWireframe01.kt)
