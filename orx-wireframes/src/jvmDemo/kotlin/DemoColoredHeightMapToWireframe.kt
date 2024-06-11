import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.camera.OrbitalControls
import org.openrndr.extra.pointclouds.ColoredHeightMapToPointCloudGenerator
import org.openrndr.extra.wireframes.ColoredPointCloudToWireframeGenerator
import org.openrndr.math.Vector3

/**
 * Demonstrates a typical usage of converting an organized and colored point cloud to a wireframe.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(4) // makes lines smoother
    }
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val earth = loadImage("demo-data/images/nasa-blue-marble.png")
        val pointCloud = ColoredHeightMapToPointCloudGenerator(heightScale = .01).generate(
            heightMap,
            colors = earth
        )
        val wireFrameGenerator = ColoredPointCloudToWireframeGenerator()
        val wireFrame = wireFrameGenerator.generate(
            pointCloud,
            colors = earth
        )
        val camera = OrbitalCamera(
            eye = Vector3(0.03, 0.03, .3),
            lookAt = Vector3.ZERO,
            near = .001
        )
        extend(camera)
        extend(OrbitalControls(camera))
        val style = shadeStyle {
            fragmentTransform = "x_fill.rgb = va_color.rgb;"
        }
        extend {
            drawer.shadeStyle = style
            drawer.vertexBuffer(wireFrame, DrawPrimitive.LINES)
        }
    }
}
