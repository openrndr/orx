package org.openrndr.extra.shapes.utilities

import org.openrndr.shape.ShapeContour

/**
 * Insert point at [t]
 * @param ascendingTs a list of ascending T values
 * @param weldEpsilon minimum distance between T values
 */
fun ShapeContour.insertPointAt(t: Double, weldEpsilon: Double = 1E-6): ShapeContour {
    val splitContours = splitAt(listOf(t), weldEpsilon)
    return ShapeContour.fromContours(splitContours, closed)
}

/**
 * Insert point at [segmentIndex], [segmentT]
 * @param weldEpsilon minimum distance between T values
 */
fun ShapeContour.insertPointAt(segmentIndex: Int, segmentT: Double, weldEpsilon: Double = 1E-6): ShapeContour {
    val t = (1.0 / segments.size) * (segmentIndex + segmentT)
    val splitContours = splitAt(listOf(t), weldEpsilon)

    return ShapeContour.fromContours(splitContours, closed)
}


/**
 * Insert points at [ascendingTs]
 * @param ascendingTs a list of ascending T values
 * @param weldEpsilon minimum distance between T values
 */
fun ShapeContour.insertPointsAt(ascendingTs: List<Double>, weldEpsilon:Double = 1E-6) : ShapeContour {
    val splitContours = splitAt(ascendingTs, weldEpsilon)
    return ShapeContour.fromContours(splitContours, closed)
}