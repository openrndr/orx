package org.openrndr.extra.mesh

/**
 * Convert mesh data to Wavefront OBJ representation
 * @param allowNonStandardBehavior if true non-standard encoding of color and tangent data is allowed
 */
fun ICompoundMeshData.toObj(allowNonStandardBehavior: Boolean = true): String {
    val sb = StringBuilder()

    require(compounds.values.all { it.vertexData == vertexData }) {
        "compounds do not share vertex data"
    }

    /*
    Output positions
     */
    if (vertexData.colors.isEmpty()) {
        for (p in vertexData.positions) {
            sb.appendLine("v ${p.x} ${p.y} ${p.z}")
        }
    } else {
        require(vertexData.positions.size == vertexData.colors.size) {
            "position and color data do not align"
        }
        for (pc in vertexData.positions.zip(vertexData.colors)) {
            sb.appendLine("v ${pc.first.x} ${pc.first.y} ${pc.first.z} ${pc.second.r} ${pc.second.g} ${pc.second.b} ${pc.second.alpha}")
        }
    }

    /*
    Output normals. Non-standard behavior where normal-tangent-bitangent is emitted as `vn`
     */
    if (!allowNonStandardBehavior || vertexData.tangents.isEmpty() || vertexData.bitangents.isEmpty()) {
        for (n in vertexData.normals) {
            sb.appendLine("vn ${n.x} ${n.y} ${n.z}")
        }
    } else {
        for (i in vertexData.normals.indices) {
            val n = vertexData.normals[i]
            val t = vertexData.tangents[i]
            val b = vertexData.bitangents[i]
            sb.appendLine("vn ${n.x} ${n.y} ${n.z} ${t.x} ${t.y} ${t.z} ${b.x} ${b.y} ${b.z}")
        }
    }

    /*
    Output texture coordinates
     */
    for (t in vertexData.textureCoords) {
        sb.appendLine("vt ${t.x} ${t.y}")
    }

    /*
    Output compounds
     */
    for (g in compounds) {
        sb.appendLine("g ${g.key}")

        /*
        Output polygons
         */
        for (p in g.value.polygons) {
            sb.appendLine("f ${
                (0 until p.positions.size).joinToString(" ") { i ->
                    listOf(
                        p.positions.getOrNull(i)?.plus(1)?.toString() ?: "",
                        p.textureCoords.getOrNull(i)?.plus(1)?.toString() ?: "",
                        p.normals.getOrNull(i)?.plus(1)?.toString() ?: ""
                    ).joinToString("/")
                }
            }")
        }
    }
    return sb.toString()
}