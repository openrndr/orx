# orx-point-clouds

3D-wireframe generating functions

## Setting up the orbital camera to view wireframes

```kotlin
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.camera.OrbitalControls
import org.openrndr.extra.pointclouds.ColoredHeightMapToPointCloudGenerator
import org.openrndr.math.Vector3

fun main() = application {
    program {
        val wireframe = wireframeVertexBuffer(IntVector2(640, 480))
        // populate wireframe with data
        val camera = OrbitalCamera(
            eye = Vector3.UNIT_Z * .5,
            lookAt = Vector3.ZERO
        )
        extend(camera)
        extend(OrbitalControls(camera))
        extend {
            drawer.vertexBuffer(wireframe, DrawPrimitive.LINES)
        }
    }
}
```
## Point clouds as an input for wireframe generation

The wireframe generators take as an input the output of generators from the [orx-point-clouds](../orx-point-clouds)
module.

## Usage

```kotlin
val wireFrameGenerator = PointCloudToWireframeGenerator()
val wireFrame = wireFrameGenerator.generate(
    getPointCloud()
)
extend {
    drawer.vertexBuffer(wireframe, DrawPrimitive.LINES)
}
```
