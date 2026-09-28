package org.openrndr.extra.camera.projection

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3

/**
 * Builds the generalized projection matrix described in
 * https://and-what-happened.blogspot.com/2010/11/ultimate-projection-calculation-almost.html
 *
 * The projection is defined by an off-axis rectangular viewport -- of size [viewportWidth] by
 * [viewportHeight], centered at [viewportCenter] -- and a center of projection [eye], both given
 * in the same (e.g. world or view) space. [eye] and [viewportCenter] only need to differ in
 * their `z` coordinate for an on-axis projection; any difference in `x`/`y` produces an
 * off-axis (sheared) frustum, which is what lets this same formula describe an asymmetric
 * viewport -- as used for multi-monitor/CAVE-style setups, where the "eye" can be off-center
 * relative to a fixed screen.
 *
 * [perspective] continuously blends between a parallel/orthographic projection (`0.0`) and a
 * full perspective projection (`1.0`); values outside that range extrapolate the blend.
 * [stereoOffset] shifts the viewport horizontally by half an eye separation for stereoscopic
 * rendering -- pass `+separation / 2.0` and `-separation / 2.0` for the two eyes of a stereo
 * pair, or `0.0` for an ordinary, single-eye projection.
 *
 * In every case the result maps [near]..[far] and the [viewportWidth]/[viewportHeight] extent
 * of the viewport rectangle (at its own depth) onto the standard `-1..1` normalized device
 * coordinate cube on all three axes; a point exactly on the viewport plane has a homogeneous
 * `w` of `1.0` after the transform.
 *
 * [near] and [far] are literal `z` coordinates in the same space as [eye] and
 * [viewportCenter], not positive distances the way e.g. `org.openrndr.math.transforms.perspective`
 * takes them -- since [eye] and [viewportCenter] can differ in `x`/`y` too, there is no single
 * "forward" axis this function could use to turn a distance into a coordinate. For a camera at
 * the origin looking down `-Z` with its near plane one unit in front of it, pass `near = -1.0`,
 * not `near = 1.0`.
 *
 * @param eye the center of projection (the "camera" position)
 * @param viewportCenter the center of the viewport rectangle, at the depth the rectangle's
 * [viewportWidth]/[viewportHeight] are measured at
 * @param viewportWidth width of the viewport rectangle
 * @param viewportHeight height of the viewport rectangle
 * @param near the near clipping plane's `z` coordinate (see above -- not a distance)
 * @param far the far clipping plane's `z` coordinate (see above -- not a distance)
 * @param perspective `0.0` for parallel (orthographic), `1.0` for perspective, or a blend
 * in between
 * @param stereoOffset a signed horizontal eye-separation offset; `0.0` for a non-stereo
 * projection
 */
fun generalizedProjection(
    eye: Vector3,
    viewportCenter: Vector3,
    viewportWidth: Double,
    viewportHeight: Double,
    near: Double,
    far: Double,
    perspective: Double = 1.0,
    stereoOffset: Double = 0.0
): Matrix44 {
    val (ex, ey, ez) = eye
    val (vx, vy, vz) = viewportCenter
    val w = viewportWidth
    val h = viewportHeight
    val n = near
    val f = far
    val p = perspective
    val s = stereoOffset

    // The distance from the eye to the viewport plane; every row below is scaled by it.
    val dz = vz - ez
    val vzp = vz * (1.0 - p) - ez

    val wx = 0.0
    val wy = 0.0
    val wz = p / dz
    val ww = vzp / dz

    val zx = 0.0
    val zy = 0.0
    val zz = (2.0 * vzp + p * (f + n)) / ((f - n) * dz)
    val zw = -(vzp * (f + n) + 2.0 * f * n * p) / ((f - n) * dz)

    val yx = 0.0
    val yy = 2.0 / h
    val yz = 2.0 * (ey - vy) / (h * dz)
    val yw = 2.0 * (vy * ez - ey * vz) / (h * dz)

    val xx = 2.0 / w
    val xy = 0.0
    val xz = (2.0 * (ex - vx) + s) / (w * dz)
    val xw = (2.0 * (vx * ez - ex * vz) - s * vz) / (w * dz)

    return Matrix44(
        xx, xy, xz, xw,
        yx, yy, yz, yw,
        zx, zy, zz, zw,
        wx, wy, wz, ww
    )
}
