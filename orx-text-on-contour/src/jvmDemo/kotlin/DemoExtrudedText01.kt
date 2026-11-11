import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.extra.textoncontour.extrudeText
import org.openrndr.extra.textoncontour.layoutTextOnPath3D
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates [extrudeText]: solid letters from a text layout, here text along a helix laid out by
 * [layoutTextOnPath3D]. Every glyph is filled, with the holes of letters cut out, and extruded along the z-axis of its
 * placement, 0.3 units deep and centered on the path. Drag with the mouse to orbit around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        // text a unit high: the face at 16 points, scaled to a sixteenth
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(16.0)
        val style = TextStyle.Defaults.copy(face = face, sizeInEm = 1.0 / 16.0)
        val helix = List(49) { i -> Vector3(cos(-i * PI / 8) * 6.0, i * 0.25 - 6.0, sin(-i * PI / 8) * 6.0) }
            .catmullRom(0.5, false).toPath3D()
        val layout = layoutTextOnPath3D(
            listOf(TextSpan("Solid letters along a helix, the holes of o and e cut out.", null)),
            helix, style = style, startDistance = 1.0
        )
        val mesh = buildTriangleMesh { extrudeText(layout, depth = 0.3) }
        val light = shadeStyle {
            fragmentTransform = """
                vec3 n = normalize(v_worldNormal);
                float d = 0.25 + 0.75 * max(0.0, dot(n, normalize(vec3(-0.4, 0.6, 0.7))));
                x_fill.rgb = vec3(0.35, 0.69, 1.0) * d;
            """
        }

        extend(Orbital()) {
            eye = Vector3(0.0, 4.0, 24.0)
            fov = 50.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.strokeWeight = 1.0
            drawer.path(helix)
            drawer.shadeStyle = light
            drawer.vertexBuffer(mesh, DrawPrimitive.TRIANGLES)
        }
    }
}
