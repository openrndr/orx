import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.computeshaders.resolution
import org.openrndr.extra.pointclouds.HeightMapToPointCloudGenerator
import org.openrndr.extra.wireframes.PointCloudToWireframeGenerator
import org.openrndr.math.Vector3

/**
 * Demonstrates a typical usage of converting an organized point cloud to a wireframe.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(4) // makes lines smoother
    }
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val pointCloud = HeightMapToPointCloudGenerator(
            heightScale = .1
        ).generate(heightMap)
        val wireFrameGenerator = PointCloudToWireframeGenerator()
        val wireFrame = wireFrameGenerator.generate(
            pointCloud,
            heightMap.resolution
        )
        extend(Orbital()) {
            eye = Vector3(0.03, 0.03, .3)
            lookAt = Vector3.ZERO
            near = .001
            keySpeed = .01
        }
        extend {
            drawer.vertexBuffer(wireFrame, DrawPrimitive.LINES)
        }
    }
}
