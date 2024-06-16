package org.openrndr.extra.objloader

import org.openrndr.draw.VertexBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An Ordered Map.
 * Container of Vec2 / Vec3 String representations.
 * Ensures no strings are duplicates (using a Map)
 * and order (using a List).
 */
private class UniqueCoords {
    // Map pointing unique strings to their index.
    // We could just use `coords.indexOf(floats)` but using a map is faster.
    private val indices = mutableMapOf<String, Int>()

    // List containing unique Strings
    private val coords = mutableListOf<String>()

    /**
     * Adds strings only if they are not found yet in `coords`.
     * Returns the index of the received argument inside `coords`.
     */
    fun add(floats: String): Int {
        val index = indices[floats]
        return if (index == null) {
            coords.add(floats)
            val newIndex = coords.size
            indices[floats] = newIndex
            newIndex
        } else index
    }

    /**
     * Returns a valid .obj block representing `coords`.
     */
    fun toObjBlock(token: String) = coords.joinToString("\n$token ", "$token ", "\n")
}

/**
 * Saves a VertexBuffer to a Wavefront OBJ file.
 * Faces use indices. Vertices, normals and texture coordinates are deduplicated.
 */
fun VertexBuffer.saveOBJ(filePath: String) {
    val bb = ByteBuffer.allocateDirect(vertexCount * vertexFormat.size)
    bb.order(ByteOrder.nativeOrder())
    read(bb)

    val tokens = mapOf(
        "position" to "v",
        "normal" to "vn",
        "texCoord0" to "vt"
    )

    val indexMap = tokens.values.associateWith { UniqueCoords() }
    val lastIndices = tokens.values.associateWith { 0 }.toMutableMap()
    val vertexIndices = mutableListOf<String>()

    // Process the ByteBuffer and populate data structures
    while (bb.position() < bb.capacity()) {
        vertexFormat.items.forEach { vertexElement ->
            val floats = List(vertexElement.type.componentCount) {
                bb.getFloat()
            }.joinToString(" ")
            val token = tokens[vertexElement.attribute]
            if (token != null) {
                lastIndices[token] = indexMap[token]!!.add(floats)
            }
        }
        vertexIndices.add("${lastIndices["v"]}/${lastIndices["vt"]}/${lastIndices["vn"]}")
    }

    val f = File(filePath)
    f.bufferedWriter().use { writer ->
        writer.run {
            // Write header
            appendLine("# OPENRNDR")
            appendLine("# www.openrndr.org")
            appendLine("o " + f.name)

            // Write v, vt, vn blocks
            indexMap.forEach { (token, verts) ->
                appendLine(verts.toObjBlock(token))

            }
            // Write faces processing three vertices at a time
            vertexIndices.chunked(3) {
                appendLine("f ${it[0]} ${it[1]} ${it[2]}")
            }
        }
    }
}