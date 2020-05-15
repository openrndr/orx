package org.openrndr.boofcv.binding

import georegression.struct.point.Point2D_F32
import georegression.struct.point.Point2D_F64
import georegression.struct.point.Point2D_I32
import org.openrndr.math.Vector2

fun Point2D_I32.toVector2() = Vector2(x.toDouble(), y.toDouble())
fun Point2D_F32.toVector2() = Vector2(x.toDouble(), y.toDouble())
fun Point2D_F64.toVector2() = Vector2(x.toDouble(), y.toDouble())
fun List<Point2D_I32>.toVector2s() = this.map { it.toVector2()  }
