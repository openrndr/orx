# orx-point-clouds

3D-point cloud generating functions

## Setting up the orbital camera to view point clouds

```kotlin
fun main() = application {
    program {
        val pointCloud = pointCloudVertexBuffer(IntVector2(640, 480))
        // populate point cloud with data
        extend(Orbital()) {
            eye = Vector3.UNIT_Z * .5
            lookAt = Vector3.ZERO            
        }
        extend {
            drawer.vertexBuffer(pointCloud, DrawPrimitive.POINTS)
        }
    }
}
```

## About organized point clouds

Organized point clouds assume that points are stored in a 2-dimensional array, for example representing XY-coordinates
of a plane, where the 3rd dimension might represent an elevation or a distance.

## Height map point clouds

### Usage

```kotlin
val heightMap = loadImage("height-map.png")
val pointCloud = HeightMapToPointCloudGenerator().generate(heightMap)
extend {
    drawer.vertexBuffer(pointCloud, DrawPrimitive.POINTS)
}
```

Note: the `heightMap` can be any `ColorBuffer`, not necessarily an image.

The `HeightMapToPointCloudGenerator` is using a `ComputeShader` under the hood, which will read the RED channel from
the `height-map.png`. The resulting `pointCloud` is a `VertexBuffer` which can be rendered as points.

### The `preserveProportions` parameter

The `preserveProportions` proportion flag can be passed to the `HeightMapToPointCloudGenerator` constructor.

By default, it is set to `true`, which preserves the original proportions of the supplied height map image, centering
the resulting point cloud in point `[0, 0, 0]` and normalizing the width in the `-1..1` range of the `X` coordinate.

When set to `false`, the `XY` coordinates of the resulting point cloud will be normalized in the `0..1` range which
might be useful for certain use case scenarios, for example for wrapping the points over a sphere in the spherical 
coordinate system.

### The `heightScale` parameter

It is possible to control the `heightScale` by supplying parameter to the `HeightMapToPointCloudGenerator` constructor.
The default `heightScale` is `1.0`, which means that maximal height value read from the source (1.0 in case of images),
will be multiplied by this factor when placing it on the Z coordinate.

### Streaming the height map data

The `generate` function produces the `pointCloud` `VertexBuffer` once. If the point cloud generation is supposed to
happen continuously (e.g. to represent some real time data as a point cloud), then the `populate` function can be used
instead:

```kotlin
val resolution = IntVector2(640, 480)
val generator = HeightMapToPointCloudGenerator()
val pointCloud = pointCloudVertexBuffer(resolution)
extend {
    generator.populate(pointCloud, getCurrentHeightMap(), resolution)
    drawer.vertexBuffer(pointCloud, DrawPrimitive.POINTS)
}
```

### Colored height maps

A special `ColoredHeightMapToPointCloudGenerator` will use `coloredPointCloudVertexBuffer()` function which also defines
color attribute for each point/vertex. It can be used as follows:

```kotlin
val generator = ColoredHeightMapToPointCloudGenerator()
val pointCloud = generator.generate(
    heightMap = getHeightMap(),
    colors = getColors()
)
val style = shadeStyle {
    fragmentTransform = "x_fill.rgb = va_color.rgb;"
}
extend {
    drawer.shadeStyle = style
    drawer.vertexBuffer(pointCloud, DrawPrimitive.POINTS)
}
```
<!-- __demos__ -->
## Demos
### DemoColoredHeightMapToPointCloud
[source code](src/jvmDemo/kotlin/DemoColoredHeightMapToPointCloud.kt)

![DemoColoredHeightMapToPointCloudKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-point-clouds/images/DemoColoredHeightMapToPointCloudKt.png)

### DemoImageToPointCloud
[source code](src/jvmDemo/kotlin/DemoImageToPointCloud.kt)

![DemoImageToPointCloudKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-point-clouds/images/DemoImageToPointCloudKt.png)

### DemoSingleLineEarth
[source code](src/jvmDemo/kotlin/DemoSingleLineEarth.kt)

![DemoSingleLineEarthKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-point-clouds/images/DemoSingleLineEarthKt.png)
