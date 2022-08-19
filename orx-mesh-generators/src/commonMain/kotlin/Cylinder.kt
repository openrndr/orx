package org.openrndr.extra.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.mix
import org.openrndr.math.transforms.rotateZ
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun cylinderMesh(
    sides: Int = 16,
    segments: Int = 16,
    radius: Double = 1.0,
    length: Double,
    invert: Boolean = false,
    center: Boolean = false,
): VertexBuffer {
    val vertexCount = 6 * sides * segments
    val vb = meshVertexBuffer(vertexCount)
    vb.put {
        generateCylinder(sides, segments, radius, length, invert, center, bufferWriter(this))
    }
    return vb
}

/**
 * Generate a cylinder along the z-axis
 * @param sides the number of sides of the cylinder
 * @param segments the number of segments along the z-axis
 * @param radius the radius of the cylinder
 * @param length the length of the cylinder
 * @param invert generates inside-out geometry if true
 * @param center
 * @param vertexWriter the vertex writer function
 */

fun generateCylinder(
    sides: Int,
    segments: Int,
    radius: Double,
    length: Double,
    invert: Boolean = false,
    center: Boolean = false,
    vertexWriter: VertexWriter
) = generateTaperedCylinder(sides, segments, radius, radius, length, invert, center, vertexWriter)

/**
 * Generate a tapered cylinder along the z-axis
 * @param sides the number of sides of the tapered cylinder
 * @param segments the number of segments along the z-axis
 * @param radiusStart the start radius of the tapered cylinder
 * @param radiusEnd the end radius of the tapered cylinder
 * @param length the length of the tapered cylinder
 * @param invert generates inside-out geometry if true
 * @param center
 * @param vertexWriter the vertex writer function
 */
fun generateTaperedCylinder(
    sides: Int,
    segments: Int,
    radiusStart: Double,
    radiusEnd: Double,
    length: Double,
    invert: Boolean = false,
    center: Boolean = false,
    vertexWriter: VertexWriter
) {
    val dphi = (PI * 2) / sides
    val ddeg = (360.0) / sides

    val invertFactor = if (invert) 1.0 else -1.0

    val dr = radiusEnd - radiusStart

    val baseNormal = Vector2(length, dr).normalized.perpendicular().let { Vector3(x = it.y, y = 0.0, z = it.x) }

    val zOffset = if (center) -length / 2.0 else 0.0


    for (segment in 0 until segments) {
        val radius0 = mix(radiusStart, radiusEnd, segment * 1.0 / segments)
        val radius1 = mix(radiusStart, radiusEnd, (segment + 1) * 1.0 / segments)
        val z0 = (length / segments) * segment + zOffset
        val z1 = (length / segments) * (segment + 1) + zOffset


        for (side in 0 until sides) {
            val x00 = cos(side * dphi) * radius0
            val x10 = cos(side * dphi + dphi) * radius0
            val y00 = sin(side * dphi) * radius0
            val y10 = sin(side * dphi + dphi) * radius0

            val x01 = cos(side * dphi) * radius1
            val x11 = cos(side * dphi + dphi) * radius1
            val y01 = sin(side * dphi) * radius1
            val y11 = sin(side * dphi + dphi) * radius1


            val u0 = (segment + 0.0) / segments
            val u1 = (segment + 1.0) / segments
            val v0 = (side + 0.0) / sides
            val v1 = (side + 1.0) / sides


            val n0 = (Matrix44.rotateZ(side * ddeg) * baseNormal.xyz0).xyz.normalized * invertFactor
            val n1 = (Matrix44.rotateZ((side + 1) * ddeg) * baseNormal.xyz0).xyz.normalized * invertFactor


            if (invert) {
                vertexWriter(Vector3(x00, y00, z0), n0, Vector2(u0, v0))
                vertexWriter(Vector3(x10, y10, z0), n1, Vector2(u0, v1))
                vertexWriter(Vector3(x11, y11, z1), n1, Vector2(u1, v1))

                vertexWriter(Vector3(x11, y11, z1), n1, Vector2(u1, v1))
                vertexWriter(Vector3(x01, y01, z1), n0, Vector2(u1, v0))
                vertexWriter(Vector3(x00, y00, z0), n0, Vector2(u0, v0))
            } else {
                vertexWriter(Vector3(x00, y00, z0), n0, Vector2(u0, v0))
                vertexWriter(Vector3(x01, y01, z1), n0, Vector2(u1, v0))
                vertexWriter(Vector3(x11, y11, z1), n1, Vector2(u1, v1))

                vertexWriter(Vector3(x11, y11, z1), n1, Vector2(u1, v1))
                vertexWriter(Vector3(x10, y10, z0), n1, Vector2(u0, v1))
                vertexWriter(Vector3(x00, y00, z0), n0, Vector2(u0, v0))
            }
        }
    }
}