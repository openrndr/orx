package org.openrndr.extra.triangulation

fun orient2d(bx: Double, by:Double, ax:Double, ay:Double, cx:Double, cy:Double) : Double {

    // (ax,ay) (bx,by) are swapped such that the sign of the determinant is flipped. which is what Delaunator.kt expects.


    /*
    | a  b | = | ax - cx  ay - cy |
    | c  d |   | bx - cx  by - cy |
    */


    val a = ax - cx
    val b = ay - cy
    val c = bx - cx
    val d = by - cy

    val determinant = ddDiffDd(twoProduct(a, d), twoProduct(b, c))

    return  determinant[1]

}