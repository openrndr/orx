package org.openrndr.extra.triangulation

fun orient2d(bx: Double, by: Double, ax: Double, ay: Double, cx: Double, cy: Double): Double {
    // (ax,ay) (bx,by) are swapped such that the sign of the determinant is flipped. which is what Delaunator.kt expects.

    /*
    | a  b | = | ax - cx  ay - cy |
    | c  d |   | bx - cx  by - cy |
    */

    val a = twoDiff(ax, cx)
    val b = twoDiff(ay, cy)
    val c = twoDiff(bx, cx)
    val d = twoDiff(by, cy)

    val determinant = ddDiffDd(ddMultDd(a, d), ddMultDd(b, c))

    return determinant[1]
}