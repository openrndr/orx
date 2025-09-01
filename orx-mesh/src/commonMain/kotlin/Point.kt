package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Represents a 3D point with optional attributes for texture coordinates, color,
 * normal vector, tangent vector, and bitangent vector.
 *
 * @property position The 3D position of the point represented as a [Vector3].
 * @property textureCoord The optional 2D texture coordinates of the point represented as a [Vector2].
 * @property color The optional color of the point represented as a [ColorRGBa].
 * @property normal The optional normal vector of the point represented as a [Vector3].
 * @property tangent The optional tangent vector of the point represented as a [Vector3].
 * @property bitangent The optional bitangent vector of the point represented as a [Vector3].
 */
data class Point(
    val position: Vector3,
    val textureCoord: Vector2? = null,
    val color: ColorRGBa? = null,
    val normal: Vector3? = null,
    val tangent: Vector3? = null,
    val bitangent: Vector3? = null
) {

    /**
     * Computes the pose of the point as a transformation matrix. The pose is derived using the
     * normalized tangent, bitangent, and normal vectors as the columns of the matrix, combined
     * with the position vector as the translation component.
     *
     * @return A 4x4 transformation matrix representing the pose of the point.
     * @throws IllegalArgumentException if the normal, tangent, or bitangent vectors are null.
     */
    fun pose(): Matrix44 {
        require(normal != null && tangent != null && bitangent != null) {
            "Normal, tangent, and bitangent must be non-null to compute the pose."
        }
        return Matrix44.fromColumnVectors(
            tangent.normalized.xyz0,
            bitangent.normalized.xyz0,
            normal.normalized.xyz0,
            position.xyz1
        )
    }
}