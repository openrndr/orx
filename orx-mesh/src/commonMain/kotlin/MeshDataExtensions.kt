package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.openrndr.shape.Box

/**
 * The [VertexFormat] for a [VertexBuffer] with positions, normals and texture coordinates.
 */
internal val objVertexFormat = vertexFormat {
    position(3)
    normal(3)
    textureCoordinate(2)
    color(4)
}

internal val objVertexFormatTangents = vertexFormat {
    position(3)
    normal(3)
    textureCoordinate(2)
    color(4)
    attribute("tangent", VertexElementType.VECTOR3_FLOAT32)
    attribute("bitangent", VertexElementType.VECTOR3_FLOAT32)
}


/**
 * Determine if [IMeshData] is triangular by checking if each polygon has exactly 3 vertices
 */
fun IMeshData.isTriangular(): Boolean {
    return polygons.all { it.positions.size == 3 }
}

/**
 * Convert a [MeshData] instance into a [VertexBuffer]
 */
fun IMeshData.toVertexBuffer(elementOffset: Int = 0, vertexBuffer: VertexBuffer? = null): VertexBuffer {
    val objects = triangulate().toPolygons()
    val triangleCount = objects.size

    val format = if (vertexData.tangents.isNotEmpty() && vertexData.bitangents.isNotEmpty()) {
        objVertexFormatTangents
    } else objVertexFormat

    val vertexBuffer = vertexBuffer ?: vertexBuffer(format, triangleCount * 3)

    vertexBuffer.put(elementOffset) {
        objects.forEach {
            for (i in it.positions.indices) {
                write(it.positions[i])
                if (it.normals.isNotEmpty()) {
                    write(it.normals[i])
                } else {
                    val d0 = it.positions[2] - it.positions[0]
                    val d1 = it.positions[1] - it.positions[0]
                    write(d0.normalized.cross(d1.normalized).normalized)
                }
                if (it.textureCoords.isNotEmpty()) {
                    write(it.textureCoords[i])
                } else {
                    write(Vector2.ZERO)
                }
                if (it.colors.isNotEmpty()) {
                    write(it.colors[i])
                } else {
                    write(ColorRGBa.WHITE)
                }
                if (format == objVertexFormatTangents) {
                    write(it.tangents[i])
                    write(it.bitangents[i])
                }
            }
        }
    }
    vertexBuffer.shadow.destroy()
    return vertexBuffer
}

/**
 * Weld vertices
 * @param positionFractBits number of bits to use for fractional representation, negative amount to skip welding
 */
fun IMeshData.weld(
    positionFractBits: Int,
    textureCoordFractBits: Int = -1,
    colorFractBits: Int = -1,
    normalFractBits: Int = -1,
    tangentFractBits: Int = -1,
    bitangentFractBits: Int = -1
): MeshData {

    fun MutableMap<IntVector3, Int>.quantize(v: Vector3, bits: Int): Int =
        getOrPut((v * (1 shl bits).toDouble()).toInt()) { this.size }

    fun MutableMap<IntVector2, Int>.quantize(v: Vector2, bits: Int): Int =
        getOrPut((v * (1 shl bits).toDouble()).toInt()) { this.size }

    fun MutableMap<IntVector4, Int>.quantize(v: Vector4, bits: Int): Int =
        getOrPut((v * (1 shl bits).toDouble()).toInt()) { this.size }

    val positionMap = mutableMapOf<IntVector3, Int>()
    val textureCoordMap = mutableMapOf<IntVector2, Int>()
    val colorMap = mutableMapOf<IntVector4, Int>()
    val normalMap = mutableMapOf<IntVector3, Int>()
    val tangentMap = mutableMapOf<IntVector3, Int>()
    val bitangentMap = mutableMapOf<IntVector3, Int>()

    if (positionFractBits >= 0) {
        for (p in vertexData.positions) {
            positionMap.quantize(p, positionFractBits)
        }
    }

    if (textureCoordFractBits >= 0) {
        for (p in vertexData.textureCoords) {
            textureCoordMap.quantize(p, textureCoordFractBits)
        }
    }

    if (colorFractBits >= 0) {
        for (p in vertexData.colors) {
            colorMap.quantize(p.toVector4(), colorFractBits)
        }
    }

    if (normalFractBits >= 0) {
        for (p in vertexData.normals) {
            normalMap.quantize(p, normalFractBits)
        }
    }

    if (tangentFractBits >= 0) {
        for (p in vertexData.tangents) {
            tangentMap.quantize(p, tangentFractBits)
        }
    }

    if (bitangentFractBits >= 0) {
        for (p in vertexData.bitangents) {
            bitangentMap.quantize(p, bitangentFractBits)
        }
    }

    val reindexedPolygons = mutableListOf<IndexedPolygon>()

    for (polygon in polygons) {
        val positions = if (positionFractBits >= 0) {
            vertexData.positions.slice(polygon.positions).map { positionMap.quantize(it, positionFractBits) }
        } else {
            polygon.positions
        }

        val textureCoords = if (textureCoordFractBits >= 0) {
            vertexData.textureCoords.slice(polygon.textureCoords)
                .map { textureCoordMap.quantize(it, textureCoordFractBits) }
        } else {
            polygon.textureCoords
        }

        val colors = if (colorFractBits >= 0) {
            vertexData.colors.slice(polygon.colors).map { colorMap.quantize(it.toVector4(), colorFractBits) }
        } else {
            polygon.colors
        }

        val normals = if (normalFractBits >= 0) {
            vertexData.normals.slice(polygon.normals).map { normalMap.quantize(it, normalFractBits) }
        } else {
            polygon.normals
        }

        val tangents = if (tangentFractBits >= 0) {
            vertexData.tangents.slice(polygon.tangents).map { tangentMap.quantize(it, tangentFractBits) }
        } else {
            polygon.tangents
        }

        val bitangents = if (bitangentFractBits >= 0) {
            vertexData.bitangents.slice(polygon.bitangents).map { bitangentMap.quantize(it, bitangentFractBits) }
        } else {
            polygon.bitangents
        }

        reindexedPolygons.add(IndexedPolygon(positions, textureCoords, colors, normals, tangents, bitangents))
    }

    val positionByIndex = vertexData.positions.associateBy { positionMap.quantize(it, positionFractBits) }
    val textureCoordByIndex =
        vertexData.textureCoords.associateBy { textureCoordMap.quantize(it, textureCoordFractBits) }
    val colorByIndex = vertexData.colors.associateBy { colorMap.quantize(it.toVector4(), colorFractBits) }
    val normalByIndex = vertexData.normals.associateBy { normalMap.quantize(it, normalFractBits) }
    val tangentByIndex = vertexData.tangents.associateBy { tangentMap.quantize(it, tangentFractBits) }
    val bitangentByIndex = vertexData.bitangents.associateBy { bitangentMap.quantize(it, bitangentFractBits) }

    val reindexedVertexData = VertexData(
        if (positionFractBits >= 0) {
            (0 until positionByIndex.size).map { positionByIndex.getValue(it) }
        } else {
            vertexData.positions
        },
        if (textureCoordFractBits >= 0) {
            (0 until textureCoordByIndex.size).map { textureCoordByIndex.getValue(it) }
        } else {
            vertexData.textureCoords
        },
        if (colorFractBits >= 0) {
            (0 until colorByIndex.size).map { colorByIndex.getValue(it) }
        } else {
            vertexData.colors
        },
        if (normalFractBits >= 0) {
            (0 until normalByIndex.size).map { normalByIndex.getValue(it) }
        } else {
            vertexData.normals
        },
        if (tangentFractBits >= 0) {
            (0 until tangentByIndex.size).map { tangentByIndex.getValue(it) }
        } else {
            vertexData.tangents
        },
        if (bitangentFractBits >= 0) {
            (0 until bitangentByIndex.size).map { bitangentByIndex.getValue(it) }
        } else {
            vertexData.bitangents
        }
    )
    return MeshData(reindexedVertexData, reindexedPolygons)
}

/**
 * Evaluate mesh bounds
 */
val IMeshData.bounds: Box
    get() = polygons.bounds(vertexData)