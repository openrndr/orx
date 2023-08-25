import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.extrudeContourStepsMorphed
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.math.PI
import kotlin.math.cos

/**
 * Demo extrudeContourStepsMorphed which allows to create a mesh with a morphing cross-section
 * based on the t value along a Path3D. In other words, a tube in which the cross-section does not need
 * to be constant, but can be scaled, rotated and displaced along its curvy axis.
 */
fun main() {
    application {
        configure {
            width = 800
            height = 800
            multisample = WindowMultisample.SampleCount(8)
        }
        program {
            Random.seed = System.currentTimeMillis().toString()

            val texture = loadImage("demo-data/images/peopleCity01.jpg").also {
                it.wrapU = WrapMode.REPEAT
                it.wrapV = WrapMode.REPEAT
                it.filterMag = MagnifyingFilter.LINEAR
                it.filterMin = MinifyingFilter.LINEAR
            }

            val shader = shadeStyle {
                fragmentTransform = """
                        // A. Passed color
                        x_fill = va_color;
                        
                        // B. Sample texture
                        //x_fill = texture(p_img, va_texCoord0.yx * vec2(20.0, 1.0));
                        
                        // Show (add) UV coords
                        x_fill.rb += va_texCoord0.yx;
                        
                        // Vertical lighting 
                        x_fill.rgb *= dot(vec3(0.0, 1.0, 0.0), v_viewNormal.xyz) * 0.3 + 0.7;
                        
                        // Black fog (darken far away shapes)
                        x_fill.rgb += v_viewPosition.z * 0.05;
                    """.trimIndent()
                parameter("img", texture)
            }

            extend(Orbital()) {
                eye = Vector3(0.0, 3.0, 7.0)
                lookAt = Vector3(0.0, 0.0, 0.0)
            }
            extend {
                drawer.stroke = null

                val path = get3DPath(10.0, seconds * 0.05, 400)
                val tubes = makeTubes(path, seconds * 0.2)

                shader.parameter("seconds", seconds * 0.1)
                drawer.fill = ColorRGBa.WHITE
                drawer.shadeStyle = shader
                tubes.forEachIndexed { i, vb ->
                    shader.parameter("offset", i * 0.3 + 0.2)

                    // Mirror the mesh 5 times
                    repeat(5) {
                        drawer.isolated {
                            rotate(Vector3.UNIT_Z, it * 72.0)
                            vertexBuffer(vb, DrawPrimitive.TRIANGLES)
                        }
                    }

                    // Remember to free the memory! Otherwise, the computer will quickly run out of RAM.
                    vb.destroy()
                }

            }
        }
    }
}

val crossSection = Circle(Vector2.ZERO, 0.1).contour.transform(
    transform { scale(5.0, 1.0, 1.0) }
)

// Create simplex-based 3D path
fun get3DPath(scale: Double, time: Double, steps: Int): Path3D {
    val mult = 0.005
    val points = List(steps) { Vector3.simplex(337, time + it * mult) * scale }
    return Path3D(points.windowed(2).map { Segment3D(it[0], it[1]) }, false)
}

// Create 3 spinning tubes around path
fun makeTubes(path: Path3D, seconds: Double) = List(3) { i ->
    buildTriangleMesh {
        val degrees = seconds * 60
        val crossSection = crossSection

        color = listOf(ColorRGBa.RED, ColorRGBa.GREEN, ColorRGBa.BLUE)[i]
        extrudeContourStepsMorphed(
            { t: Double ->
                val turns = t * 360 * 10
                val cosEnv = 0.5 - 0.49 * cos(t * 2 * PI)
                crossSection.transform(transform {
                    val theta = i * 120.0
                    translate(Polar(turns - degrees + theta, 0.6 * cosEnv).cartesian)
                    rotate(-turns - degrees)
                    scale(cosEnv)
                })
            },
            path,
            path.segments.size,
            Vector3.UNIT_Y,
            contourDistanceTolerance = 0.01
        )
    }
}
