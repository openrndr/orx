package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
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
    val bitangent: Vector3? =null
)