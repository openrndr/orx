package org.openrndr.extra.shapes.deform

import org.openrndr.extra.shapes.distort.distort
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.pow



// --- the main entry point -----------------------------------------------

/**
 * Approximates the shape [curve] would take if every point on it were pulled
 * toward [attractor] by a distance-weighted force, with both endpoints pinned.
 * Adaptively splits into more pieces wherever a single cubic Bezier can't fit
 * the displacement field within [errorTolerance].
 *
 * Displacement field (defined over the GLOBAL parameter t of the input curve,
 * so taper only varies along the true curve, not per split piece):
 *
 *   D(t) = B(t) + strength * taper(t) * fall(t) * (attractor - B(t))
 *   taper(t) = (1 - endpointPinning) + endpointPinning * 4t(1-t)
 *   fall(t)  = 1 / (1 + (dist(t) / refDistance)^falloff)
 *
 * taper(t) blends between two extremes:
 *   endpointPinning = 0 -> taper(t) = 1 everywhere, so the endpoints are pulled
 *                          exactly as strongly (relative to their own distance)
 *                          as any interior point - the whole curve is "attracted".
 *   endpointPinning = 1 -> taper(t) = 4t(1-t), which is 0 at t=0,1, reproducing
 *                          the original pinned-rod behavior (fixed endpoints).
 * Values in between give partial resistance at the ends.
 *
 * Each candidate piece is fit with its endpoints FIXED to the true displaced
 * position at its t-range boundaries (not the original curve's position) so
 * neighboring pieces always join up exactly, regardless of endpointPinning.
 *
 * @param curve           original cubic Bezier
 * @param attractor       the attracting point
 * @param strength        overall pull strength; ~0 = no bend, ~1 = strong bend
 * @param falloff         distance falloff exponent (0 = uniform, 2 = gravity-like)
 * @param refDistance     normalizes falloff so it doesn't blow up near the attractor
 * @param endpointPinning 0 = endpoints move like any other point (fully attracted),
 *                        1 = endpoints stay fixed in place (fully pinned), values
 *                        in between blend smoothly. Default is fully attracted.
 * @param errorTolerance  max allowed RMS fit error (same units as the curve's
 *                        coordinates) before a piece is split in two
 * @param maxDepth        recursion limit; caps output at 2^maxDepth pieces.
 *                        Keep this modest (5-8) — it's a hard ceiling, not a target.
 * @param samplesPerPiece interior samples used both for fitting and for measuring
 *                        the resulting error
 */
fun Segment2D.deform(
    attractor: Vector2,
    strength: Double = 1.0,
    falloff: Double = 2.0,
    refDistance: Double = 1.0,
    endpointPinning: Double = 0.0,
    errorTolerance: Double = 0.5,
    maxDepth: Int = 6,
    samplesPerPiece: Int = 24
): List<Segment2D> = distort(errorTolerance, maxDepth, samplesPerPiece) { base, t ->
    val bump = 4.0 * t * (1.0 - t)
    val taper = (1.0 - endpointPinning) + endpointPinning * bump
    val toAttractor = attractor - base
    val dist = toAttractor.length
    val fall = 1.0 / (1.0 + (dist / refDistance).pow(falloff))
    base + toAttractor * (strength * taper * fall)
}
