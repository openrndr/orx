package org.openrndr.extra.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.math.*
import org.openrndr.math.transforms.rotateY

/**
 * A shape created by rotating an envelope around a vertical axis.
 * The default envelope is a horizontal line which produces a flat round disk.
 * By providing a more complex envelope one can create curved shapes like a bowl.
 */
fun capMesh(
    sides: Int,
    radius: Double,
    envelope: List<Vector2> = listOf(
        Vector2(0.0, 0.0),
        Vector2(1.0, 0.0)
    )
): VertexBuffer {
    val vb = meshVertexBuffer(6 * sides * (envelope.size - 1))
    vb.put {
        generateCap(sides, radius, envelope, bufferWriter(this))
    }
    return vb
}

fun generateCap(
    sides: Int,
    radius: Double,
    envelope: List<Vector2> = listOf(
        Vector2(0.0, 0.0),
        Vector2(1.0, 0.0)
    ),
    writer: VertexWriter
) {
    val maxX = envelope.maxByOrNull { it.x } ?: Vector2(1.0, 0.0)
    val a = maxX.x

    val cleanEnvelope = envelope.map { Vector2((it.x / a) * radius, it.y) }

    val normals2D = envelope.zipWithNext().map {
        val d = it.second - it.first
        d.normalized.perpendicular(YPolarity.CCW_POSITIVE_Y)
    }

    val basePositions = cleanEnvelope.map { Vector3(it.x, it.y, 0.0) }
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

/**
 * A shape created by rotating an envelope around a vertical axis.
 * The default envelope is a vertical line which produces a hollow cylinder.
 */
fun revolveMesh(
    sides: Int,
    length: Double,
    envelope: List<Vector2> = listOf(
        Vector2(1.0, 0.0),
        Vector2(1.0, 1.0)
    )
): VertexBuffer {
    val vb = meshVertexBuffer(6 * sides * (envelope.size - 1))
    vb.put {
        generateRevolve(sides, length, envelope, bufferWriter(this))
    }
    return vb
}

fun generateRevolve(
    sides: Int,
    length: Double,
    envelope: List<Vector2> = listOf(
        Vector2(1.0, 0.0),
        Vector2(1.0, 1.0)
    ),
    writer: VertexWriter
) {
    val maxY = envelope.maxByOrNull { it.y } ?: Vector2(0.0, 1.0)
    val a = maxY.y

    val cleanEnvelope = envelope.map { Vector2((it.x), (it.y/a - 0.5) * length ) }

    val normals2D = envelope.zipWithNext().map {
        val d = it.second - it.first
        d.normalized.perpendicular() * Vector2(1.0, -1.0)
    }

    val basePositions = cleanEnvelope.map { Vector3(it.x, it.y, 0.0) }
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