import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.spaces.ColorOKHSVa
import org.openrndr.extra.noise.poissonDiskSampling
import org.openrndr.extra.shapes.Arrangement
import org.openrndr.extra.shapes.BoundedFace
import org.openrndr.extra.shapes.hobbyCurve
import kotlin.random.Random

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        // Create a nice curve that intersects itself
        val uniformPoints = poissonDiskSampling(drawer.bounds.offsetEdges(-200.0), 100.0, random=Random(10579))
        val curve = hobbyCurve(uniformPoints, closed=true)

        // Construct an arrangement of the curve. In order to obtain an arrangement dealing with self intersections,
        // the curve is passed in twice.
        val arrangement = Arrangement(curve, curve)

        // We will color each bounded face.
        val faces = arrangement.boundedFaces
        val colors = faces.withIndex().associate { (i, f) ->
            f to ColorOKHSVa(i * 360.0 / faces.size, 0.75, 1.0).toRGBa()
        }

        extend {
            drawer.apply {
                clear(ColorRGBa.WHITE)

                isolated {
                    // Shrink the drawing
                    translate(drawer.bounds.center)
                    scale(0.5)
                    translate(-drawer.bounds.center)

                    // Draw each face
                    stroke = null
                    for (f in faces) {
                        fill = colors[f]
                        contour(f.contour)
                    }

                    // Draw the curve on top
                    fill = null
                    stroke = ColorRGBa.BLACK
                    strokeWeight = 4.0
                    contour(curve)

                    strokeWeight = 4.0
                    stroke = ColorRGBa.BLACK
                    fill = ColorRGBa.WHITE
                    circles(arrangement.vertices.map { it.pos }, 12.0)
                }

                // We are going to draw the neighborhood of each vertex in the arrangement
                for (v in arrangement.vertices) {
                    isolated {
                        // Shrink the drawing quite a bit
                        translate(v.pos)
                        scale(0.35)
                        translate(-v.pos)

                        // Move the drawing in the direction of the vertex
                        translate((v.pos - drawer.bounds.center).normalized * 300.0)

                        // For each outgoing half-edge, draw the associated face
                        for (e in v.outgoing) {
                            val f = e.face as? BoundedFace
                            if (f != null) {
                                stroke = null
                                fill = colors[f]!!.opacify(0.5)
                                contour(f.contour)
                            }
                        }

                        // For each outgoing half-edge, draw the edge
                        for (e in v.outgoing) {
                            strokeWeight = 2.0/0.35
                            stroke = ColorRGBa.BLACK
                            contour(e.contour)
                        }

                        // Draw the vertex
                        strokeWeight = 2 / 0.35
                        stroke = ColorRGBa.BLACK
                        fill = ColorRGBa.WHITE
                        circle(v.pos, 6.0 / 0.35)
                    }
                }
            }
        }
    }
}