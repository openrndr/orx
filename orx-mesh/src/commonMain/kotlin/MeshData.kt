package org.openrndr.extra.mesh

import kotlin.jvm.JvmRecord


/**
 * Interface representing mesh data in 3D space.
 *
 * Provides access to vertices and polygonal structure, along with methods
 * for common mesh transformations and manipulations.
 */
interface IMeshData {
    /**
     * Provides vertex data for the mesh, including positions, normals, colors,
     * texture coordinates, tangents, and bitangents. This data is central to
     * defining the geometric and visual properties of the mesh and can be used
     * for performing various operations and transformations.
     */
    val vertexData: IVertexData
    /**
     * Represents the list of indexed polygons that define the structure of the mesh.
     *
     * Each polygon in the list is an instance of [IIndexedPolygon], which references
     * vertex attributes through indices, such as positions, texture coordinates, normals, and more.
     */
    val polygons: List<IIndexedPolygon>


    /**
     * Converts the current mesh data into a fully triangulated form.
     *
     * This method processes the mesh's polygons and ensures that all non-triangle polygons
     * are subdivided into triangles. The resulting mesh maintains the original structure
     * and attributes while adhering to the requirement of being composed solely of triangles.
     *
     * @return A new instance of [IMeshData] containing the triangulated representation
     *         of the original mesh data.
     */
    fun triangulate(): IMeshData


    /**
     * Converts the current mesh data into a list of polygons.
     *
     * This method extracts the polygonal structure of the mesh and represents
     * it as a collection of [IPolygon] instances. Each polygon contains vertex
     * data such as positions, texture coordinates, colors, normals, tangents,
     * and bitangents, which are used to define its geometry and visual properties.
     *
     * @return A list of [IPolygon] instances representing the individual polygons
     *         within the mesh.
     */
    fun toPolygons(): List<IPolygon>


    /**
     * Combines the current mesh data with another mesh data instance.
     *
     * The method merges the polygons and vertex attributes of the two meshes, resulting in a new
     * mesh data instance that includes the data from both inputs.
     *
     * @param other The [IMeshData] instance to be merged with the current mesh data.
     * @return A new [IMeshData] instance that contains the combined data from both meshes.
     */
    fun join(other: IMeshData): IMeshData


    /**
     * Converts the current mesh data into an immutable `MeshData` instance.
     *
     * This method provides a direct representation of the current mesh,
     * encapsulating its vertex data and polygon information in an immutable format.
     *
     * @return A `MeshData` instance representing the current mesh data.
     */
    fun toMeshData(): MeshData

    /**
     * Converts the current mesh data into a mutable representation.
     *
     * This method provides a `MutableMeshData` instance that encapsulates
     * the current mesh data, allowing modifications to its vertex and polygon
     * structures. The mutable representation is useful for scenarios where
     * changes to the mesh data are required, such as editing geometry or
     * updating attributes.
     *
     * @return A `MutableMeshData` instance that represents the current mesh
     *         data in a mutable format.
     */
    fun toMutableMeshData() : MutableMeshData
}

/**
 * Represents data for a 3D mesh. Implements the `IMeshData` interface and provides additional methods
 * for manipulating and combining mesh data. This class is immutable and includes operations for
 * triangulation, conversion to polygons, and joining multiple meshes.
 *
 * @property vertexData The vertex data associated with the mesh, including positions, normals, tangents,
 *                      texture coordinates, colors, and bitangents.
 * @property polygons A list of polygons defined using indexed vertex data.
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
 * Represents mutable mesh data with modifiable vertex data and polygonal structure.
 *
 * @property vertexData Mutable vertex data instance containing positions, texture coordinates,
 *                      colors, normals, tangents, and bitangents of vertices.
 * @property polygons Mutable list of indexed polygons defining the geometric structure of the mesh.
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