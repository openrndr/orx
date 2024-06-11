import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.camera.OrbitalControls
import org.openrndr.extra.pointclouds.ColoredHeightMapToPointCloudGenerator
import org.openrndr.math.Vector3

/**
 * Renders rotating Earth as a sphere made out of a single line, with exaggerated elevation data.
 *
 * This demonstrates that `VertexBuffers`s containing points can be either drawn as individual points or
 * as a continuous line according to the order of points.
 *
 * Note: using a vertex shader for putting points on the surface of a sphere is not the most efficient way of delivering
 * such an effect. Using a compute shader doing such a point placement directly would be more efficient.
 */
fun main() = application {
    program {
        val earth = loadImage("demo-data/images/nasa-blue-marble.png")
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val generator = ColoredHeightMapToPointCloudGenerator(
            preserveProportions = false, // important to ease mapping of 0..1 ranges to spherical coordinates in radians
            heightScale = .1,
        )
        val pointCloud = generator.generate(
            heightMap = heightMap,
            colors = earth
        )
        val camera = OrbitalCamera(
            eye = Vector3.UNIT_Y * 1.6,
            lookAt = Vector3.ZERO
        )
        extend(camera)
        extend(OrbitalControls(camera))
        val style = shadeStyle {
            vertexPreamble = "const float PI = 3.14159265359;"
            vertexTransform = """
                float phi = a_position.x * PI * 2.0;
                float theta = a_position.y * PI;
                x_position = vec3(
                    sin(theta) * cos(phi),
                    sin(theta) * sin(phi),
                    cos(theta)
                ) * (1.0 + a_position.z);
                """.trimIndent()
            fragmentTransform = "x_fill.rgb = va_color.rgb;"
        }
        extend {
            drawer.run {
                shadeStyle = style
                rotate(Vector3.UNIT_Z, seconds * 5.0)
                vertexBuffer(pointCloud, DrawPrimitive.LINE_STRIP)
            }
        }
    }
}
