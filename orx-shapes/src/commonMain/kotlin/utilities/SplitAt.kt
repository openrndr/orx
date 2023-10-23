package org.openrndr.extra.shapes.utilities

import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour

fun ShapeContour.splitAt(segmentIndex: Double, segmentT: Double): List<ShapeContour> {
    val t = (1.0 / segments.size) * (segmentIndex + segmentT)
    return splitAt(listOf(t))
}

fun ShapeContour.splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<ShapeContour> {
    if (empty || ascendingTs.isEmpty()) {
        return listOf(this)
    }
    @Suppress("NAME_SHADOWING") val ascendingTs = (listOf(0.0) + ascendingTs + listOf(1.0)).weldAscending(weldEpsilon)
    return ascendingTs.windowed(2, 1).map {
        sub(it[0], it[1])
    }
}

fun Segment.splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<Segment> {
    if (ascendingTs.isEmpty()) {
        return listOf(this)
    }

    @Suppress("NAME_SHADOWING") val ascendingTs = (listOf(0.0) + ascendingTs + listOf(1.0)).weldAscending(weldEpsilon)
    return ascendingTs.windowed(2, 1).map {
        sub(it[0], it[1])
    }
}