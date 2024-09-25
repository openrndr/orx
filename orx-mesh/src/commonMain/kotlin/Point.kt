package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Point with optional attributes
 * @param position position attribute
 * @param textureCoord optional texture coordinate attribute
 * @param color optional color attribute
 * @param normal optional normal attribute
 * @param tangent optional tangent attribute
 * @param bitangent optional bitangent attribute
 */
data class Point(
    val position: Vector3,
    val textureCoord: Vector2? = null,
    val color: ColorRGBa? = null,
    val normal: Vector3? = null,
    val tangent: Vector3? = null,
    val bitangent: Vector3? =null
)