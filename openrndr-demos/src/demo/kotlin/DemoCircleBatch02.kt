import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.circleBatch
import org.openrndr.draw.shadeStyle

/*
This program demonstrates creating "pre-baked" batches of circles.
Batches can have varying fill, stroke and strokeWeight settings.

Batches are (currently) static but stored in GPU memory but can be
animated using a vertex shader. Batches are fast to draw.
*/

fun main() = application {
    program {
        val batch = drawer.circleBatch {
            for (i in 0 until 2000) {
                fill = ColorRGBa.PINK.shade(Math.random())
                strokeWeight = Math.random() * 5
                circle(width * 0.5, height * 0.5, 20 * Math.random() + 5)
            }
        }

        extend {
            drawer.clear(ColorRGBa.GRAY)

            // The following optional shadeStyle animates the batch
            // by using polar coordinates:
            // sets angle and radius based on time and shape ID.
            drawer.shadeStyle = shadeStyle {
                vertexTransform = """
                    float a = c_instance + p_time * 0.1;
                    float r = 200 + 100 * sin(a * 0.998);
                    x_position.x += r * sin(a);
                    x_position.y += r * cos(a);
                """.trimIndent()
                parameter("time", seconds)
            }

            drawer.circles(batch)
        }
    }
}