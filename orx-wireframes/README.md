# orx-point-clouds

3D-wireframe generating functions

## Setting up the orbital camera to view wireframes

```kotlin
fun main() = application {
    program {
        val resolution = IntVector2(640, 480)
        val wireframe = wireframeVertexBuffer(resolution)
        // populate wireframe with data
        extend(Orbital()) {
            eye = Vector3.UNIT_Z * .5
            lookAt = Vector3.ZERO            
        }
        extend {
            drawer.vertexBuffer(wireframe, DrawPrimitive.LINES)
        }
    }
}
```

## Point clouds as an input for wireframe generation

The wireframe generators take as an input the output of generators from the [orx-point-clouds](../orx-point-clouds)
module.

## Generating single wireframe

```kotlin
val wireFrameGenerator = PointCloudToWireframeGenerator()
val wireFrame = wireFrameGenerator.generate(
    getPointCloud()
)
extend {
    drawer.vertexBuffer(wireframe, DrawPrimitive.LINES)
}
```

## Streaming continuously updated point cloud data

```kotlin
val resolution = IntVector2(640, 480)
val wireframe = wireframeVertexBuffer(resolution)
val wireFrameGenerator = PointCloudToWireframeGenerator()

extend {
    wireFrameGenerator.populate(
        wireframe,
        getCurrentPointCloud(),
        resolution
    )
    drawer.vertexBuffer(wireframe, DrawPrimitive.LINES)
}
```

## Colored wireframes

Optional color information associated with points in the point cloud can be also used to color the wireframe. For this
purpose use `ColoredPointCloudToWireframeGenerator`.

<!-- __demos__ -->
## Demos
### DemoColoredHeightMapToWireframe
[source code](src/jvmDemo/kotlin/DemoColoredHeightMapToWireframe.kt)

![DemoColoredHeightMapToWireframeKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-wireframes/images/DemoColoredHeightMapToWireframeKt.png)

### DemoHeightMapToWireframe
[source code](src/jvmDemo/kotlin/DemoHeightMapToWireframe.kt)

![DemoHeightMapToWireframeKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-wireframes/images/DemoHeightMapToWireframeKt.png)

### DemoWireframeEarth
[source code](src/jvmDemo/kotlin/DemoWireframeEarth.kt)

![DemoWireframeEarthKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-wireframes/images/DemoWireframeEarthKt.png)
