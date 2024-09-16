package org.openrndr.extra.objloader

/**
 * Compound mesh data interface
 */
interface ICompoundMeshData {
    val vertexData: IVertexData
    val compounds: Map<String, IMeshData>

    fun triangulate(): ICompoundMeshData
}

class CompoundMeshData(
    override val vertexData: VertexData,
    override val compounds: Map<String, MeshData>
) : ICompoundMeshData {

    init {
        
    }

    override fun triangulate(): CompoundMeshData {
        return CompoundMeshData(vertexData, compounds.mapValues {
            it.value.triangulate()
        })
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
            }.toMutableMap())
    }
}