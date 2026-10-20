package org.openrndr.extra.shapes.elastica

import org.openrndr.extra.shapes.mvc.Fairness
import org.openrndr.extra.shapes.mvc.fairSegments
import org.openrndr.extra.shapes.ph.cubicApproximation
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.ShapeContour

/**
 * Converts this polyline into an elastica through all points: a minimum-energy curve (MEC), the G2 curve that
 * minimizes its bending energy ∫κ² ds, which is the shape of a thin elastic rod threaded through pins at the points. A
 * pin applies a force but no bending moment, so the curvature is continuous where the rod passes it.
 *
 * The bending energy shrinks as a curve grows, so where the curve loops it has no minimum: the loop would inflate
 * without end. The scale-invariant form, the energy times the curve length, is minimized instead, after Moreton and
 * Séquin, "Functional optimization for fair surface design" (SIGGRAPH 1992). By the Cauchy-Schwarz inequality that is
 * at least (∫|κ| ds)², with equality only for constant curvature, so points on a circle give exactly that circle.
 * Collinear points give a straight line, and the ends of an open polyline are free like the ends of a rod.
 *
 * The curve is built and optimized like [org.openrndr.extra.shapes.mvc.minimumVariationCurve]: quintic Béziers that
 * share the tangent and curvature at their joints, optimized with Levenberg-Marquardt, with edges that turn far split
 * into several quintics. Compared to the minimum-variation curve, which spreads the change of curvature as evenly as
 * it can, the elastica keeps the curvature itself low, so it bends more where it has to and runs straighter in between.
 *
 * The optimization is local, so for erratic points the result is the nearest local minimum, which is still G2 and
 * passes through all points.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param tolerance the largest distance between the cubic approximation and the quintic segments
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.elastica(
    closed: Boolean,
    tolerance: Double = 0.01,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    val segments = fairSegments(closed, Fairness.ENERGY)
    if (segments.isEmpty()) {
        return ShapeContour.fromPoints(this, closed, polarity)
    }
    val cubics = segments.flatMap { segment -> cubicApproximation(segment::position, segment::derivative, tolerance) }
    return ShapeContour.fromSegments(cubics, closed, polarity)
}
