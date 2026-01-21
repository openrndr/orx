package org.openrndr.extra.shapes.loft

import org.openrndr.extra.shapes.functions.poseFunction
import org.openrndr.extra.shapes.functions.positionFunction
import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.extra.math.differential.diff
import org.openrndr.extra.math.differential.diffdx
import org.openrndr.extra.math.differential.diffdy

class Loft(
    val crossSection: (u: Double, v: Double) -> Vector2,
    val dcrossSectiondu: (u: Double, v: Double) -> Vector2 = crossSection.diffdx(),
    val dcrossSectiondv: (u: Double, v: Double) -> Vector2 = crossSection.diffdy(),
    val pose: (v: Double) -> Matrix44,
    val dposedv: (v: Double) -> Matrix44 = pose.diff(),
) {
    fun position(u: Double, v: Double): Vector3 {
        val pose = pose(v)
        val point = crossSection(u, v)

        val n = pose[0].xyz
        val b = pose[1].xyz

        return pose[3].xyz + n * point.x + b * point.y
    }

    fun normal(u: Double, v: Double): Vector3 {
        val pose = pose(v)
        val dposedv = dposedv(v)

        val n = pose[0].xyz
        val b = pose[1].xyz

        val dndv = dposedv[0].xyz
        val dbdv = dposedv[1].xyz
        val dcdv = dposedv[3].xyz

        val p = crossSection(u, v)
        val dpdu = dcrossSectiondu(u, v)
        val dpdv = dcrossSectiondv(u, v)

        val dsdu = n * dpdu.x + b * dpdu.y
        val dsdv = dcdv + n * dpdv.x + dndv * p.x + b * dpdv.y + dbdv * p.y

        return dsdu.cross(dsdv)
    }
}

fun RectifiedContour.loft(pose: PosePath3D): Loft {
    return Loft(
        crossSection = { u: Double, _: Double -> this.positionFunction(u) },
        dcrossSectiondv = { _: Double, _: Double -> Vector2.ZERO },
        pose = pose.poseFunction
    )
}