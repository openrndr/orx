package org.openrndr.extras.meshgenerators

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.rotateY

fun generateCap(sides: Int, radius: Double, enveloppe: List<Vector2> = listOf(Vector2(0.0, 0.0), Vector2(1.0, 0.0)), writer: VertexWriter) {
    val maxX = enveloppe.maxBy { it.x } ?: Vector2(1.0, 0.0)
    val a = maxX.x

    val cleanEnveloppe = enveloppe.map { Vector2((it.x / a) * radius, it.y) }

    val normals2D = enveloppe.zipWithNext().map {
        val d = it.second - it.first
        d.normalized.perpendicular
    }

    val basePositions = cleanEnveloppe.map { Vector3(it.x, it.y, 0.0) }
    val baseNormals = normals2D.map { Vector3(it.x, it.y, 0.0) }

    for (side in 0 until sides) {
        val r0 = Matrix44.rotateY(360.0 / sides * side)
        val r1 = Matrix44.rotateY(360.0 / sides * (side + 1))

        val v0 = basePositions.map { (r0 * it.xyz0).xyz }
        val v1 = basePositions.map { (r1 * it.xyz0).xyz }
        val n0 = baseNormals.map { (r0 * it.xyz0).xyz }
        val n1 = baseNormals.map { (r1 * it.xyz0).xyz }

        for (segment in 0 until basePositions.size - 1) {

            val p00 = v0[segment]
            val p01 = v0[segment+1]
            val p10 = v1[segment]
            val p11 = v1[segment+1]

            val nn0 = n0[segment]
            val nn1 = n1[segment]

            writer(p00, nn0, Vector2.ZERO)
            writer(p01, nn0, Vector2.ZERO)
            writer(p11, nn1, Vector2.ZERO)

            writer(p11, nn1, Vector2.ZERO)
            writer(p10, nn1, Vector2.ZERO)
            writer(p00, nn0, Vector2.ZERO)
        }
    }
}

fun generateRevolve(sides: Int, length: Double, enveloppe: List<Vector2> = listOf(Vector2(1.0, 0.0), Vector2(1.0, 1.0)), writer: VertexWriter) {
    val maxY = enveloppe.maxBy { it.y } ?: Vector2(0.0, 1.0)
    val a = maxY.y

    val cleanEnveloppe = enveloppe.map { Vector2((it.x), (it.y/a - 0.5) * length ) }

    val normals2D = enveloppe.zipWithNext().map {
        val d = it.second - it.first
        d.normalized.perpendicular * Vector2(1.0, -1.0)

    }

    val extended = listOf(normals2D[0]) + normals2D + normals2D[normals2D.size-1]

    val basePositions = cleanEnveloppe.map { Vector3(it.x, it.y, 0.0) }
    val baseNormals = normals2D.map { Vector3(it.x, it.y, 0.0) }

    for (side in 0 until sides) {
        val r0 = Matrix44.rotateY(360.0 / sides * side)
        val r1 = Matrix44.rotateY(360.0 / sides * (side + 1))

        val v0 = basePositions.map { (r0 * it.xyz0).xyz }
        val v1 = basePositions.map { (r1 * it.xyz0).xyz }
        val n0 = baseNormals.map { (r0 * it.xyz0).xyz }
        val n1 = baseNormals.map { (r1 * it.xyz0).xyz }

        for (segment in 0 until basePositions.size - 1) {

            val p00 = v0[segment]
            val p01 = v0[segment+1]
            val p10 = v1[segment]
            val p11 = v1[segment+1]

            val nn0 = n0[segment]
            val nn1 = n1[segment]

            writer(p00, nn0, Vector2.ZERO)
            writer(p10, nn1, Vector2.ZERO)
            writer(p11, nn1, Vector2.ZERO)

            writer(p11, nn1, Vector2.ZERO)
            writer(p01, nn0, Vector2.ZERO)

            writer(p00, nn0, Vector2.ZERO)
        }
    }
}