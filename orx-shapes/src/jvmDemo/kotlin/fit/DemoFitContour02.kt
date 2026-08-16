package fit

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.shapes.fit.fitCubicBeziers
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.floor

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {
            class AnimContour(val contour: RectifiedContour, val spawnTime: Double) {
                val delta = contour.position(1.0) - contour.position(0.0)

                fun update(seconds: Double) : ShapeContour {
                    val dt = seconds - spawnTime
                    val n = floor(dt)

                    val tr0 = transform {
                        translate(delta * n)
                    }

                    val tr1 = transform {
                        translate(delta * (n+1))
                    }


                    val t = dt.mod(1.0)
                    val c =  contour.sub(t, 1.0).transform(tr0) + contour.sub(0.0, t).transform(tr1)

                    val cc = contour.contour.transform(tr0) + contour.contour.transform(tr1)

                    val b = cc.bounds
                    
                    val r = Rectangle(0.0, 0.0, width.toDouble(), height.toDouble())

                    val cbCenterX = ((b.center.x - r.x).mod(r.width)) + r.x
                    val cbCenterY = ((b.center.y - r.y).mod(r.height)) + r.y
                    val cb = Rectangle.fromCenter(Vector2(cbCenterX, cbCenterY), b.width, b.height)

                    val dx = cb.center.x - b.center.x
                    val dy = cb.center.y - b.center.y

                    return c.transform(
                        transform {
                            translate(dx, dy)
                        }
                    )

                }
            }

            val contours = mutableListOf<ShapeContour>()
            val animContours = mutableListOf<AnimContour>()
            val points = mutableListOf<Vector2>()

            var hackSeconds = seconds

            mouse.dragged.listen {

                if (points.isEmpty() || points.last().distanceTo(it.position) > 10.0) {
                    points.add(mouse.position)
                }
            }
            mouse.buttonUp.listen {
                if (points.size >= 2) {
                    val segments = fitCubicBeziers(points, minPointsToSplit = 5)
                    val c = ShapeContour.fromSegments(segments, false)
                    points.clear()
                    contours.add(c)
                    animContours.add(AnimContour(contours.last().rectified(), hackSeconds))
                }
            }


            extend {
                hackSeconds = seconds

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.drawStyle.lineJoin = LineJoin.ROUND

                if (points.size > 2) {
                    val segments = fitCubicBeziers(points, minPointsToSplit = 5)
                    val c = ShapeContour.fromSegments(segments, false)
                    drawer.contour(c)

                }
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK

                val cs = animContours.map { it.update(seconds) }

                drawer.strokeWeight = 4.0

                val offsets =
                    listOf(
                        Vector2(width * -1.0, 0.0),
                        Vector2(width * 1.0, 0.0),
                        Vector2(width * -1.0, height * 1.0),
                        Vector2(width * 1.0, height * 1.0),
                        Vector2(width * -1.0, height * -1.0),
                        Vector2(width * 1.0, height * -1.0),

                        Vector2(0.0, height * -1.0),
                        Vector2(0.0, height * 1.0),
                        Vector2(0.0, 0.0))

                for (o in offsets) {
                    drawer.translate(o)
                    drawer.contours(cs)
                    drawer.translate(-o)
                }

                drawer.strokeWeight = 1.0
                drawer.circles(points, 5.0)
                drawer.circle(mouse.position, 5.0)
            }
        }
    }
}