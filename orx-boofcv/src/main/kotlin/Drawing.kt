package org.openrndr.boofcv.binding

import georegression.struct.line.LineSegment2D_F32
import georegression.struct.line.LineSegment2D_F64
import georegression.struct.trig.Circle2D_F32
import georegression.struct.trig.Circle2D_F64
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

fun Drawer.lineSegment(segment: LineSegment2D_F32) {
    lineSegment(
        segment.a.x.toDouble(),
        segment.a.y.toDouble(),
        segment.b.x.toDouble(),
        segment.b.y.toDouble()
    )
}

@JvmName("lineSegments2D_F32")
fun Drawer.lineSegments(segments: List<LineSegment2D_F32>) {
    lineSegments(
        segments.flatMap { segment ->
            listOf(
                Vector2(segment.a.x.toDouble(), segment.a.y.toDouble()),
                Vector2(segment.b.x.toDouble(), segment.b.y.toDouble())
            )
        }
    )
}

fun Drawer.lineSegment(segment: LineSegment2D_F64) {
    lineSegment(
        segment.a.x,
        segment.a.y,
        segment.b.x,
        segment.b.y
    )
}

@JvmName("lineSegments2D_F64")
fun Drawer.lineSegments(segments: List<LineSegment2D_F64>) {
    lineSegments(
        segments.flatMap { segment ->
            listOf(
                Vector2(segment.a.x, segment.a.y),
                Vector2(segment.b.x, segment.b.y)
            )
        }
    )
}

fun Drawer.circle(circle: Circle2D_F32) {
    circle(
        circle.center.x.toDouble(), circle.center.y.toDouble(),
        circle.radius.toDouble()
    )
}

fun Drawer.circle(circle: Circle2D_F64) {
    circle(
        circle.center.x, circle.center.y,
        circle.radius
    )
}

@JvmName("circles2D_F32")
fun Drawer.circles(circles: List<Circle2D_F32>) {
    circles(
        circles.map {
            Circle(it.center.x.toDouble(), it.center.y.toDouble(), it.radius.toDouble())
        }
    )
}

@JvmName("circles2D_F64")
fun Drawer.circles(circles: List<Circle2D_F64>) {
    circles(
        circles.map {
            Circle(it.center.x.toDouble(), it.center.y.toDouble(), it.radius.toDouble())
        }
    )
}