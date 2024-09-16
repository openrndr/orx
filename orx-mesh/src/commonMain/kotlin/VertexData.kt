package org.openrndr.extra.objloader

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Vertex data interface
 */
interface IVertexData {
    /**
     * Vertex positions
     */
    val positions: List<Vector3>

    /**
     * Vertex normals
     */
    val normals: List<Vector3>

    /**
     * Vertex texture coordinates
     */
    val textureCoords: List<Vector2>

    /**
     * Vertex colors
     */
    val colors: List<ColorRGBa>

    /**
     * Vertex tangents
     */
    val tangents: List<Vector3>

    /**
     * Vertex bitangents
     */
    val bitangents: List<Vector3>
}

/**
 * Immutable vertex data implementation
 */
class VertexData(
    override val positions: List<Vector3> = emptyList(),
    override val normals: List<Vector3> = emptyList(),
    override val textureCoords: List<Vector2> = emptyList(),
    override val colors: List<ColorRGBa> = emptyList(),
    override val tangents: List<Vector3> = emptyList(),
    override val bitangents: List<Vector3> = emptyList()
) : IVertexData {

    /**
     * Convert to [MutableVertexData]
     */
    fun toMutableVertexData(): MutableVertexData {
        return MutableVertexData(
            positions.toMutableList(),
            normals.toMutableList(),
            textureCoords.toMutableList(),
            colors.toMutableList(),
            tangents.toMutableList(),
            bitangents.toMutableList()
        )
    }
}


/**
 * Mutable vertex data implementation
 */
class MutableVertexData(
    override val positions: MutableList<Vector3> = mutableListOf(),
    override val normals: MutableList<Vector3> = mutableListOf(),
    override val textureCoords: MutableList<Vector2> = mutableListOf(),
    override val colors: MutableList<ColorRGBa> = mutableListOf(),
    override val tangents: MutableList<Vector3> = mutableListOf(),
    override val bitangents: MutableList<Vector3> = mutableListOf()
) : IVertexData {

    /**
     * Convert to [VertexData]
     */
    fun toVertexData(): VertexData {
        return VertexData(
            positions.toList(),
            normals.toList(),
            textureCoords.toList(),
            colors.toList(),
            tangents.toList(),
            bitangents.toList()
        )
    }
}