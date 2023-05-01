import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extra.meshgenerators.toMesh
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment

/**
 * Demonstrates how to convert a [Segment] into a
 * vertex buffer to be drawn as a TRIANGLE_STRIP
 * of constant width.
 */
fun main() = application {
    program {
        // Segments can have 2, 3 or 4 vertices
        val seg = Segment(
            Vector2(100.0, 100.0),
            Vector2(width - 100.0, 100.0),
            Vector2(100.0, height - 100.0),
            Vector2(width - 100.0, height - 100.0)
        )
        val geometry = seg.toMesh(20) { 20.0 }

        extend {
            drawer.clear(ColorRGBa.PINK)

            // Visualize the texture coordinates.
            // They can be used to map a texture into the mesh.
            //drawer.shadeStyle = shadeStyle {
            //    fragmentTransform = """
            //        x_fill.rg = va_texCoord0;
            //    """.trimIndent()
            //}

            drawer.vertexBuffer(geometry, DrawPrimitive.TRIANGLE_STRIP)
        }
    }
}