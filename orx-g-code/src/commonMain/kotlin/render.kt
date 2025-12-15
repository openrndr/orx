package org.openrndr.extra.gcode

import org.openrndr.extra.composition.Composition
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

/**
 * Renders the [composition] to the generator context.
 */
fun GeneratorContext.render(layer: String, composition: Composition) {
    beginLayer(layer, composition)
    composition.findShapes().forEachIndexed { index, shapeNode ->
        render(shapeNode.shape)
    }
    endLayer(layer, composition)
}

fun GeneratorContext.render(shape: Shape) {
    beginShape(shape)
    shape.contours.forEach { render(it) }
    endShape(shape)
}

/**
 * Renders the contour to the generator context.
 *
 * NOOP when the contour has no segments.
 */
fun GeneratorContext.render(contour: ShapeContour) {
    val start = contour.segments.firstOrNull()?.start ?: return
    beginContour(start, contour)

    with(contour.segments) {
        val isDot = size == 1 && first().start.squaredDistanceTo(first().end) < minSquaredDistance
        if (!isDot) {
            flatMap { it.points(distanceTolerance) }
                .removeClosePoints(minSquaredDistance)
                .forEach { drawTo(it) }
        }
    }

    endContour(contour)
}

/**
 * Returns the points of the segment, excluding the start point.
 * Bezier segments are approximated with adaptive positions.
 */
fun Segment2D.points(distanceTolerance: Double) = if (control.isEmpty()) {
    listOf(end)
} else {
    adaptivePositions(distanceTolerance).drop(1)
}

fun List<Vector2>.removeClosePoints(minSquaredDistance: Double) = fold(emptyList<Vector2>()) { acc, v ->
    val dist = acc.lastOrNull()?.squaredDistanceTo(v) ?: Double.POSITIVE_INFINITY
    if ( dist >= minSquaredDistance) {
        acc + v
    } else {
        acc
    }
}
