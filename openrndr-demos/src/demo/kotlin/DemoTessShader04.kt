import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.camera.OrbitalCamera
import org.openrndr.extras.meshgenerators.sphereMesh
import org.openrndr.math.Vector3
import org.openrndr.resourceUrl
import org.openrndr.shape.Ellipse
import org.openrndr.shape.path3D

import org.openrndr.extra.shaderphrases.preprocessedFromUrls
import kotlin.math.cos

fun main() {
    application {
        program {
            extend(Orbital())

            val path = path3D {
                moveTo(Vector3.ZERO)
                for (i in 0 until 100) {
                    continueTo(anchor + Vector3.uniformRing(0.0, 10.0),anchor + Vector3.uniformRing(0.0, 10.0))
                }
            }
            val vb = vertexBuffer(vertexFormat {
                position(3)
            }, path.segments.size * 4)

            val shader = Shader.preprocessedFromUrls(
                    vsUrl = resourceUrl("/shaders/ts-04.vert"),
                    tcsUrl = resourceUrl("/shaders/ts-04.tesc"),
                    tesUrl = resourceUrl("/shaders/ts-04.tese"),
                    gsUrl = resourceUrl("/shaders/ts-04.geom"),
                    fsUrl = resourceUrl("/shaders/ts-04.frag")
            )

            val mesh = sphereMesh()
            extend {
                val vc = vb.put {
                    for (segment in path.sub(0.0, cos(seconds*0.1)*0.5+ 0.5).segments) {
                        val cubic = segment.cubic
                        write(cubic.start)
                        write(cubic.control[0])
                        write(cubic.control[1])
                        write(cubic.end)
                    }
                }
                drawer.clear(ColorRGBa.PINK)
                drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL
                drawer.depthWrite = true
                drawer.vertexBuffer(mesh, DrawPrimitive.TRIANGLES)

                shader.begin()
                shader.uniform("offset", mouse.position.xy0)
                shader.uniform("view", drawer.view)
                shader.uniform("proj", drawer.projection)
                shader.uniform("model", drawer.model)
                shader.uniform("resolution", 32)
                shader.uniform("weight", 3.0 + cos(seconds))
                shader.uniform("time", seconds*0.0)
                drawer.depthWrite = false

                driver.setState(drawer.drawStyle)
                driver.drawVertexBuffer(shader, listOf(vb), DrawPrimitive.PATCHES, 0, vc)
                shader.end()
                drawer.fill = ColorRGBa.WHITE
            }
        }
    }
}