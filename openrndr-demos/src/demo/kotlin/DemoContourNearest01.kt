import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.simplex
import org.openrndr.shape.OrientedEllipse

fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = null

                val offset = seconds * 0.1
                val contours = listOf(
                        OrientedEllipse(
                                simplex(320, offset)*width/2.0 + width/2.0,
                                simplex(3120, offset)*height/2.0 + height/2.0,
                                simplex(3420, offset)*50.0 + 80.0,
                                simplex(7521, offset)*50.0+ 80.0,
                                simplex(3212, offset)*180.0+180.0
                        ).contour,
                        OrientedEllipse(
                                simplex(5320, offset)*width/2.0 + width/2.0,
                                simplex(73120, offset)*height/2.0 + height/2.0,
                                simplex(23420, offset)*50.0 + 80.0,
                                simplex(47521, offset)*50.0+ 80.0,
                                simplex(33212, offset)*180.0+180.0
                        ).contour
                )
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                for (contour in contours) {
                    drawer.contour(contour)
                }

                for (j in contours.indices) {
                    for (i in 0 until j) {
                        val eqj = contours[j].equidistantPositions(50)
                        val eqi = contours[i].equidistantPositions(50)

                        for (p in eqj) {
                            val q = contours[i].nearest(p).position
                            drawer.lineSegment(p, q)
                        }
                        for (p in eqi) {
                            val q = contours[j].nearest(p).position
                            drawer.lineSegment(p, q)
                        }
                    }
                }
            }
        }
    }
}