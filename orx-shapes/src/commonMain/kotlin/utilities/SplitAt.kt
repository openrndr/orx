package org.openrndr.extra.shapes.utilities

import org.openrndr.math.EuclideanVector
import org.openrndr.shape.*

fun ShapeContour.splitAt(segmentIndex: Double, segmentT: Double): List<ShapeContour> {
    val t = (1.0 / segments.size) * (segmentIndex + segmentT)
    return splitAt(listOf(t))
}

fun Path3D.splitAt(segmentIndex: Double, segmentT: Double): List<Path3D> {
    val t = (1.0 / segments.size) * (segmentIndex + segmentT)
    return splitAt(listOf(t))
}


fun <T : EuclideanVector<T>> Path<T>.splitAtBase(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<Path<T>> {
    if (empty || ascendingTs.isEmpty()) {
        return listOf(this)
    }
    @Suppress("NAME_SHADOWING") val ascendingTs = (listOf(0.0) + ascendingTs + listOf(1.0)).weldAscending(weldEpsilon)
    return ascendingTs.windowed(2, 1).map {
        sub(it[0], it[1])
    }
}

fun ShapeContour.splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<ShapeContour> {
    @Suppress("UNCHECKED_CAST")
    return splitAtBase(ascendingTs, weldEpsilon) as List<ShapeContour>
}

fun Path3D.splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<Path3D> {
    @Suppress("UNCHECKED_CAST")
    return splitAtBase(ascendingTs, weldEpsilon) as List<Path3D>
}

fun <T : EuclideanVector<T>> BezierSegment<T>.splitAtBase(
    ascendingTs: List<Double>,
    weldEpsilon: Double = 1E-6
): List<BezierSegment<T>> {
    if (ascendingTs.isEmpty()) {
        return listOf(this)
    }

    @Suppress("NAME_SHADOWING") val ascendingTs = (listOf(0.0) + ascendingTs + listOf(1.0)).weldAscending(weldEpsilon)
    return ascendingTs.windowed(2, 1).map {
        sub(it[0], it[1])
    }
}

fun Segment2D.splitAt(ascendingTs: List<Double>,
                      weldEpsilon: Double = 1E-6) : List<Segment2D> {
    @Suppress("UNCHECKED_CAST")
    return splitAtBase(ascendingTs, weldEpsilon) as List<Segment2D>
}

fun Segment3D.splitAt(ascendingTs: List<Double>,
                      weldEpsilon: Double = 1E-6) : List<Segment3D> {
    @Suppress("UNCHECKED_CAST")
    return splitAtBase(ascendingTs, weldEpsilon) as List<Segment3D>
}