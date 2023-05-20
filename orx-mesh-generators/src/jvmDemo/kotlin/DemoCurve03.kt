import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.meshgenerators.toMesh
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour

/**
 * Demonstrates how to convert shapes like a [Rectangle] and a [Circle]
 * into a vertex buffer to be drawn as a TRIANGLE_STRIP mesh.
 * Meshes can be drawn in 3D and deformed with vertex shaders.
 */
fun main() = application {
    program {
        val rect = Rectangle.fromCenter(drawer.bounds.center, 350.0).contour
        val cir = Circle(drawer.bounds.center, 130.0).contour

        val rectMesh = rect.toMesh(100) { 40.0 }
        val cirMesh = cir.toMesh(100) { 20.0 }
        extend {
            drawer.clear(ColorRGBa.PINK)

            // Uses the texture coordinates to create an animated pattern
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    x_fill.a = step(0.5, fract(va_texCoord0.x * 10.0 + p_seconds));
                """.trimIndent()
                parameter("seconds", seconds)
            }

            drawer.vertexBuffer(rectMesh, DrawPrimitive.TRIANGLE_STRIP)
            drawer.vertexBuffer(cirMesh, DrawPrimitive.TRIANGLE_STRIP)
        }
    }
}