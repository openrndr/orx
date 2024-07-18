import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.computeshaders.resolution
import org.openrndr.extra.meshgenerators.PointCloudToMeshGenerator
import org.openrndr.extra.pointclouds.HeightMapToPointCloudGenerator
import org.openrndr.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Presents organized point cloud as a mesh.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(4) // makes lines smoother
    }
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val resolution = heightMap.resolution
        val pointCloud = HeightMapToPointCloudGenerator(
            heightScale = .02
        ).generate(heightMap)
        val meshGenerator = PointCloudToMeshGenerator()
        val mesh = meshGenerator.generate(pointCloud, resolution)
        val style = shadeStyle {
            fragmentTransform = """
                vec3 lightDir = normalize(p_lightPosition);
                float luma = dot(va_normal, lightDir) * 0.6 + .4;
                x_fill.rgb = vec3(luma);
            """.trimIndent()
        }
        extend(Orbital()) {
            eye = Vector3(0.03, 0.03, .3)
            lookAt = Vector3.ZERO
            near = .001
            keySpeed = .01
        }
        extend {
            style.parameter("lightPosition", Vector3(
                sin(seconds) * 1.0,
                cos(seconds) * 1.0,
                1.0)
            )
            style.parameter("seconds", seconds)
            drawer.shadeStyle = style
            drawer.vertexBuffer(mesh, DrawPrimitive.TRIANGLES)
        }
    }
}
