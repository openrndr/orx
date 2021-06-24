package org.openrndr.boofcv.binding

import georegression.struct.affine.Affine2D_F32
import georegression.struct.affine.Affine2D_F64
import org.openrndr.math.Matrix44

fun Affine2D_F32.toMatrix44() = Matrix44(
        c0r0 = a11.toDouble(), c1r0 = a12.toDouble(), c3r0 = tx.toDouble(),
        c0r1 = a21.toDouble(), c1r1 = a22.toDouble(), c3r1 = ty.toDouble(),
        c2r2 = 1.0,
        c3r3 = 1.0
)

fun Affine2D_F64.toMatrix44() = Matrix44(
        c0r0 = a11, c1r0 = a12, c3r0 = tx,
        c0r1 = a21, c1r1 = a22, c3r1 = ty,
        c2r2 = 1.0,
        c3r3 = 1.0
)