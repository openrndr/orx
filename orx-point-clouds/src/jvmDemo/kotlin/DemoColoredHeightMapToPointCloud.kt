import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.camera.OrbitalControls
import org.openrndr.extra.pointclouds.ColoredHeightMapToPointCloudGenerator
import org.openrndr.math.Vector3

/**
 * Combines the height map and the color map of 2 registered images take from the NASA Blue Marble project, representing
 * the topography and colors of the Earth's surface.
 */
fun main() = application {
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val earth = loadImage("demo-data/images/nasa-blue-marble.png")
        val generator = ColoredHeightMapToPointCloudGenerator(heightScale = 0.1)
        val pointCloud = generator.generate(
            heightMap = heightMap,
            colors = earth
        )
        val camera = OrbitalCamera(
            eye = Vector3(0.03, 0.03, .3),
            lookAt = Vector3.ZERO
        )
        extend(camera)
        extend(OrbitalControls(camera))
        val style = shadeStyle {
            fragmentTransform = "x_fill.rgb = va_color.rgb;"
        }
        extend {
            drawer.shadeStyle = style
            drawer.vertexBuffer(pointCloud, DrawPrimitive.POINTS)
        }
    }
}
