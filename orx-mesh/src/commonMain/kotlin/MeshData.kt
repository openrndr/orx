package org.openrndr.extra.mesh

import kotlin.jvm.JvmRecord

/**
 * Mesh data interface
 */
interface IMeshData {
    val vertexData: IVertexData
    val polygons: List<IIndexedPolygon>

    /**
     * Convert mesh data to triangular mesh data
     */
    fun triangulate(): IMeshData

    /**
     * Convert mesh data to a list of [IPolygon]
     */
    fun toPolygons(): List<IPolygon>

    /**
     * Join mesh data with [other] mesh data
     */
    fun join(other: IMeshData): IMeshData


    fun toMeshData(): MeshData

    fun toMutableMeshData() : MutableMeshData
}

/**
 * Immutable mesh data implementation
 */
@JvmRecord
data class MeshData(
    override val vertexData: VertexData,
    override val polygons: List<IndexedPolygon>,
) : IMeshData {
    override fun triangulate(): MeshData {
        return if (isTriangular()) {
            this
        } else
            copy(polygons = polygons.flatMap { polygon -> polygon.triangulate(vertexData) })
    }

    override fun toPolygons(): List<Polygon> {
        return polygons.map { ip ->
            ip.toPolygon(vertexData)
        }
    }

    override fun join(other: IMeshData): IMeshData {
        if (vertexData === other.vertexData) {
            @Suppress("UNCHECKED_CAST")
            return MeshData(vertexData, polygons + (other.polygons as List<IndexedPolygon>))
        } else {
            val positionsShift: Int
            val positions = if (vertexData.positions === other.vertexData.positions) {
                positionsShift = 0
                vertexData.positions
            } else {
                positionsShift = vertexData.positions.size
                vertexData.positions + other.vertexData.positions
            }

            val textureCoordsShift: Int
            val textureCoords = if (vertexData.textureCoords === other.vertexData.textureCoords) {
                textureCoordsShift = 0
                vertexData.textureCoords
            } else {
                textureCoordsShift = vertexData.textureCoords.size
                vertexData.textureCoords + other.vertexData.textureCoords
            }

            val colorsShift: Int
            val colors = if (vertexData.colors === other.vertexData.colors) {
                colorsShift = 0
                vertexData.colors
            } else {
                colorsShift = vertexData.colors.size
                vertexData.colors + other.vertexData.colors
            }

            val normalsShift: Int
            val normals = if (vertexData.normals === other.vertexData.normals) {
                normalsShift = 0
                vertexData.normals
            } else {
                normalsShift = vertexData.normals.size
                vertexData.normals + other.vertexData.normals
            }

            val tangentsShift: Int
            val tangents = if (vertexData.tangents === other.vertexData.tangents) {
                tangentsShift = 0
                vertexData.tangents
            } else {
                tangentsShift = vertexData.tangents.size
                vertexData.tangents + other.vertexData.tangents
            }

            val bitangentsShift: Int
            val bitangents = if (vertexData.bitangents === other.vertexData.bitangents) {
                bitangentsShift = 0
                vertexData.bitangents
            } else {
                bitangentsShift = vertexData.bitangents.size
                vertexData.bitangents + other.vertexData.bitangents
            }

            return MeshData(
                VertexData(
                    positions = positions,
                    textureCoords = textureCoords,
                    colors = colors,
                    normals = normals,
                    tangents = tangents,
                    bitangents = bitangents
                ),
                polygons + other.polygons.map {
                    (it as IndexedPolygon).shiftIndices(
                        positionsShift,
                        textureCoordsShift,
                        colorsShift,
                        normalsShift,
                        tangentsShift,
                        bitangentsShift
                    )
                }
            )
        }
    }

    override fun toMeshData(): MeshData = this

    override fun toMutableMeshData(): MutableMeshData {
        TODO("Not yet implemented")
    }
}


/**
 * Mutable mesh data implementation
 */
data class MutableMeshData(
    override val vertexData: MutableVertexData,
    override val polygons: MutableList<IndexedPolygon>
) : IMeshData {
    override fun triangulate(): MutableMeshData {
        return if (isTriangular()) {
            this
        } else {
            copy(polygons = polygons.flatMap { it.triangulate(vertexData) }.toMutableList())
        }
    }

    override fun toPolygons(): List<Polygon> {
        return polygons.map { it.toPolygon(vertexData) }
    }

    override fun join(other: IMeshData): IMeshData {
        TODO("Not yet implemented")
    }

    override fun toMutableMeshData(): MutableMeshData {
return this
    }

    override fun toMeshData(): MeshData {
        TODO("Not yet implemented")
    }
}