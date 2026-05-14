package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import org.openrndr.shape.bounds
import kotlin.math.abs

private enum class EventType { LEFT, RIGHT }

private class Event(val x: Double, val type: EventType, val segment: Segment) : Comparable<Event> {
    override fun compareTo(other: Event): Int {
        if (abs(x - other.x) > 1e-9) return x.compareTo(other.x)
        if (type != other.type) return type.compareTo(other.type)
        return segment.left.y.compareTo(other.segment.left.y)
    }
}

private class Segment(val p0: Vector2, val p1: Vector2) {
    val left: Vector2
    val right: Vector2

    init {
        if (p0.x < p1.x || (p0.x == p1.x && p0.y < p1.y)) {
            left = p0
            right = p1
        } else {
            left = p1
            right = p0
        }
    }

    fun yAt(x: Double): Double {
        if (left.x == right.x) return left.y
        val t = (x - left.x) / (right.x - left.x)
        return left.y + t * (right.y - left.y)
    }

    fun shareEndpoint(other: Segment): Boolean {
        return left == other.left || left == other.right || right == other.left || right == other.right
    }
}

fun Polygon2D.intersectsSweep(other: Polygon2D): Boolean {
    val bounds = bounds
    val otherBounds = other.bounds

    if (!bounds.intersects(otherBounds))
        return false

    val segments = ArrayList<Segment>(points.size + other.points.size)
    for (i in points.indices) {
        segments.add(Segment(points[i], points[(i + 1) % points.size]))
    }
    for (i in other.points.indices) {
        segments.add(Segment(other.points[i], other.points[(i + 1) % other.points.size]))
    }

    val events = PriorityQueue<Event>(segments.size * 2) { a, b -> a!!.compareTo(b!!) }
    for (s in segments) {
        events.add(Event(s.left.x, EventType.LEFT, s))
        events.add(Event(s.right.x, EventType.RIGHT, s))
    }

    var x = 0.0
    val activeSegments = TreeSet<Segment> { a, b ->
        val y1 = a.yAt(x)
        val y2 = b.yAt(x)
        if (abs(y1 - y2) < 1e-9) {
            val dy1 = (a.right.y - a.left.y) / (a.right.x - a.left.x).let { if (abs(it) < 1e-12) 1e-12 else it }
            val dy2 = (b.right.y - b.left.y) / (b.right.x - b.left.x).let { if (abs(it) < 1e-12) 1e-12 else it }
            if (abs(dy1 - dy2) < 1e-12) {
                val h1 = a.hashCode()
                val h2 = b.hashCode()
                if (h1 == h2) {
                    val c = a.left.x.compareTo(b.left.x)
                    if (c == 0) a.left.y.compareTo(b.left.y) else c
                } else h1.compareTo(h2)
            } else dy1.compareTo(dy2)
        } else y1.compareTo(y2)
    }

    while (events.size > 0) {
        val event = events.poll() ?: break
        val s = event.segment
        x = event.x

        if (event.type == EventType.LEFT) {
            activeSegments.add(s)
            val prev = activeSegments.lower(s)
            val next = activeSegments.higher(s)
            if (prev != null && segmentsIntersect(prev.p0, prev.p1, s.p0, s.p1) && !prev.shareEndpoint(s)) return true
            if (next != null && segmentsIntersect(next.p0, next.p1, s.p0, s.p1) && !next.shareEndpoint(s)) return true
        } else {
            val prev = activeSegments.lower(s)
            val next = activeSegments.higher(s)
            if (prev != null && next != null && segmentsIntersect(prev.p0, prev.p1, next.p0, next.p1) && !prev.shareEndpoint(next)) return true
            activeSegments.remove(s)
        }
    }

    // if there are no intersecting edges, check if either polygon fully contains the other polygon
    if (this.points.isNotEmpty()) {
        val p = this.points[0]
        if (other.isPointInConcavePolygon(p)) {
            return true
        }
    }

    if (other.points.isNotEmpty()) {
        val p = other.points[0]
        if (this.isPointInConcavePolygon(p)) {
            return true
        }
    }

    return false
}

