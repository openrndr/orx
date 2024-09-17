package org.openrndr.extra.mesh

/**
 * Compound mesh data interface
 */
interface ICompoundMeshData {
    val vertexData: IVertexData
    val compounds: Map<String, IMeshData>

    fun triangulate(): ICompoundMeshData

    fun toMeshData(): IMeshData
}

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