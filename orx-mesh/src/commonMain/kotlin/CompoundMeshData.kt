package org.openrndr.extra.mesh


/**
 * Represents a compound mesh data structure that combines multiple meshes and their associated vertex data.
 *
 * This interface is used to handle scenarios where a collection of meshes, grouped under named compounds,
 * is operated upon as a single entity. Common operations include combining the meshes into a single one or
 * refining them into triangular meshes.
 */
interface ICompoundMeshData {
    val vertexData: IVertexData
    val compounds: Map<String, IMeshData>

    fun triangulate(): ICompoundMeshData

    fun toMeshData(): IMeshData
}

/**
 * Represents a compound mesh data structure containing multiple named sub-meshes
 * and their associated shared vertex data. This class allows manipulation of
 * grouped meshes as a single entity and provides methods for operations like
 * triangulation and converting the meshes to a single data structure.
 *
 * @property vertexData The shared vertex data associated with the compound meshes.
 * @property compounds A map of named sub-meshes where the key represents the compound name
 * and the value represents the corresponding mesh data.
 */
class CompoundMeshData(
    override val vertexData: VertexData,
    override val compounds: Map<String, MeshData>
) : ICompoundMeshData {

    override fun triangulate(): CompoundMeshData {
        return CompoundMeshData(vertexData, compounds.mapValues {
            it.value.triangulate()
        })
    }

    override fun toMeshData(): MeshData {
        return MeshData(vertexData, compounds.values.flatMap { it.polygons })
    }
}

/**
 * A mutable implementation of a compound mesh data structure, combining vertex data and a set of named mesh compounds.
 *
 * This class allows the manipulation of multiple meshes grouped under named compounds, while sharing a common
 * vertex data structure. It supports operations such as triangulation and conversion to a unified mesh representation.
 *
 * @property vertexData The mutable vertex data shared across the compound meshes.
 * @property compounds A mutable mapping of compound names to their associated mutable mesh data.
 */
class MutableCompoundMeshData(
    override val vertexData: MutableVertexData,
    override val compounds: MutableMap<String, MutableMeshData>
) : ICompoundMeshData {

    override fun triangulate(): MutableCompoundMeshData {
        return MutableCompoundMeshData(
            vertexData,
            compounds.mapValues {
                it.value.triangulate()
            }.toMutableMap()
        )
    }

    override fun toMeshData(): IMeshData {
        return MutableMeshData(vertexData, compounds.values.flatMap { it.polygons }.toMutableList())
    }
}