import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.pointclouds.HeightMapToPointCloudGenerator
import org.openrndr.math.Vector3

/**
 * Demonstrates a typical usage of converting a height map image to an organized point cloud.
 */
fun main() = application {
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val pointCloud = HeightMapToPointCloudGenerator(heightScale = .1).generate(heightMap)
        extend(Orbital()) {
            eye = Vector3(0.03, 0.03, .3)
            lookAt = Vector3.ZERO
            near = 0.001
            keySpeed = .01
        }
        extend {
            drawer.vertexBuffer(pointCloud, DrawPrimitive.POINTS)
        }
    }
}
