import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.meshgenerators.toMesh
import org.openrndr.extra.shapes.hobbyCurve
import org.openrndr.math.Polar
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos

/**
 * Demonstrates how to convert a [ShapeContour] into a
 * vertex buffer to be drawn as a TRIANGLE_STRIP.
 * Uses a cosine wave to specify the thickness.
 */
fun main() = application {
    program {
        val contour = ShapeContour.fromPoints(
            List(10) {
                Polar(
                    it * 36.0,
                    120.0 + (it % 2) * 80.0
                ).cartesian + drawer.bounds.center
            }, true
        ).hobbyCurve()

        val geometry = contour.toMesh(100) { t ->
            30.0 + 20.0 * cos(t * PI * 6)
        }
        extend {
            drawer.clear(ColorRGBa.PINK)

            // Visualize the texture coordinates
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