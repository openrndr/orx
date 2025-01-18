package org.openrndr.extra.shapes.utilities

import org.openrndr.math.EuclideanVector
import org.openrndr.shape.*

/**
 * Splits the current `ShapeContour` into multiple contours at the specified segment index and position within the segment.
 *
 * @param segmentIndex the index of the segment where the split should occur
 * @param segmentT the normalized position (0.0 to 1.0) within the segment where the split should occur
 * @return a list of `ShapeContour` objects resulting from the split
 */
fun ShapeContour.splitAt(segmentIndex: Double, segmentT: Double): List<ShapeContour> {
    val t = (1.0 / segments.size) * (segmentIndex + segmentT)
    return splitAt(listOf(t))
}

fun Path3D.splitAt(segmentIndex: Double, segmentT: Double): List<Path3D> {
    val t = (1.0 / segments.size) * (segmentIndex + segmentT)
    return splitAt(listOf(t))
}


/**
 * Splits the path at the given normalized parameter values (`ascendingTs`) and returns a list of subpaths.
 *
 * @param ascendingTs a list of normalized parameter values (from 0.0 to 1.0) specifying where the path should be split.
 * The list must be in ascending order.
 * @param weldEpsilon a small tolerance value used to merge closely adjacent parameter values; defaults to 1E-6.
 * @return a list of subpaths representing the segments of the original path split at the specified parameter values.
 * If the path is empty or `ascendingTs` is empty, the method returns a list containing the original path.
 */
fun <T : EuclideanVector<T>> Path<T>.splitAtBase(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<Path<T>> {
    if (empty || ascendingTs.isEmpty()) {
        return listOf(this)
    }
    @Suppress("NAME_SHADOWING") val ascendingTs = (listOf(0.0) + ascendingTs + listOf(1.0)).weldAscending(weldEpsilon)
    return ascendingTs.windowed(2, 1).map {
        sub(it[0], it[1])
    }
}

/**
 * Splits the current `ShapeContour` at specified normalized parameter values and returns sub-contours.
 *
 * @param ascendingTs a list of normalized parameter values (from 0.0 to 1.0) where the contour will be split.
 * The values must be in ascending order.
 * @param weldEpsilon a small tolerance value to merge closely adjacent parameter values; defaults to 1E-6.
 * @return a list of `ShapeContour` objects representing the segments of the original contour split at the specified
 * parameter values. If the contour is empty or `ascendingTs` is empty, returns the original contour as a single item in the list.
 */
fun ShapeContour.splitAt(ascendingTs: List<Double>, weldEpsilon: Double = 1E-6): List<ShapeContour> {
    @Suppress("UNCHECKED_CAST")
    return splitAtBase(ascendingTs, weldEpsilon) as List<ShapeContour>
}

/**
 * Splits the current Path3D into multiple subpaths at specified normalized parameter values.
 *
 * @param ascendingTs a list of normalized parameter values (ranging from 0.0 to 1.0) indicating where the path should be split.
 * The list must be in ascending order.
 * @param weldEpsilon a small tolerance value used to merge closely adjacent parameter values. The default value is 1E-6.
 * @return a list of Path3D objects representing the segments of the original path split at the specified parameter values.
 * If the path is empty or the `ascendingTs` list is empty, the method returns a list containing the original path.
 */
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

/**
 * Splits the current `Segment2D` instance at specified parameter values.
 *
 * @param ascendingTs a list of parameter values in ascending order where the segment
 *                    should be split. These should be in the range [0.0, 1.0].
 * @param weldEpsilon a tolerance value used to merge parameter values that are
 *                     too close to each other. Defaults to 1E-6.
 * @return a list of `Segment2D` parts obtained by splitting the original segment
 *         at the given parameter values.
 */
fun Segment2D.splitAt(ascendingTs: List<Double>,
                      weldEpsilon: Double = 1E-6) : List<Segment2D> {
    @Suppress("UNCHECKED_CAST")
    return splitAtBase(ascendingTs, weldEpsilon) as List<Segment2D>
}

/**
 * Splits the current 3D segment into multiple sub-segments at the specified parameter `t` values.
 *
 * @param ascendingTs a list of `t` values between 0.0 and 1.0, in ascending order, where the segment should be split
 * @param weldEpsilon a small tolerance value to merge very close `t` values, default is 1E-6
 * @return a list of sub-segments of type `Segment3D` resulting from the splits
 */
fun Segment3D.splitAt(ascendingTs: List<Double>,
                      weldEpsilon: Double = 1E-6) : List<Segment3D> {
    @Suppress("UNCHECKED_CAST")
    return splitAtBase(ascendingTs, weldEpsilon) as List<Segment3D>
}