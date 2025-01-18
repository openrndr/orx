package org.openrndr.extra.objloader

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.extra.mesh.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Reads and processes mesh data from a list of lines in OBJ format.
 *
 * @param lines An iterable collection of strings representing the lines of an OBJ file.
 * Each line contains information about vertices, normals, texture coordinates,
 * face definitions, or group definitions.
 * @return A `CompoundMeshData` object containing processed vertex data and meshes.
 * The resulting data includes vertices, texture coordinates, colors, normals, tangents, and bitangents,
 * alongside their associated face indices, grouped into meshes.
 */
fun readObjMeshData(lines: Iterable<String>): CompoundMeshData {
    val meshes = mutableMapOf<String, List<IndexedPolygon>>()
    val positions = mutableListOf<Vector3>()
    val normals = mutableListOf<Vector3>()
    val tangents = mutableListOf<Vector3>()
    val bitangents = mutableListOf<Vector3>()
    val textureCoords = mutableListOf<Vector2>()
    val colors = mutableListOf<ColorRGBa>()
    var activeMesh = mutableListOf<IndexedPolygon>()

    lines.forEach { line ->
        if (line.isNotEmpty()) {
            val tokens = line.split(Regex("[ |\t]+")).map { it.trim() }.filter { it.isNotEmpty() }

            if (tokens.isNotEmpty()) {
                when (tokens[0]) {
                    "v" -> {
                        when (tokens.size) {
                            3, 4 -> {
                                positions += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                            }

                            6 -> {
                                positions += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                                colors += ColorRGBa(tokens[4].toDouble(), tokens[5].toDouble(), tokens[6].toDouble())
                            }

                            7 -> {
                                positions += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                                colors += ColorRGBa(
                                    tokens[4].toDouble(),
                                    tokens[5].toDouble(),
                                    tokens[6].toDouble(),
                                    tokens[7].toDouble()
                                )
                            }

                            else -> error("vertex has ${tokens.size - 1} components, loader only supports 3/4/6/7 components")
                        }
                    }

                    "vn" -> {
                        when (tokens.size) {
                            3, 4 -> normals += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                            9 -> {
                                normals += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                                tangents += Vector3(tokens[4].toDouble(), tokens[5].toDouble(), tokens[6].toDouble())
                                bitangents += Vector3(tokens[7].toDouble(), tokens[8].toDouble(), tokens[9].toDouble())
                            }

                        }
                    }

                    "vt" -> textureCoords += Vector2(tokens[1].toDouble(), tokens[2].toDouble())
                    "g" -> {
                        activeMesh = mutableListOf()
                        meshes[tokens.getOrNull(1) ?: "no-name-${meshes.size}"] = activeMesh
                    }

                    "f" -> {
                        val indices = tokens.subList(1, tokens.size).map { it.split("/") }.map {
                            it.map { it.toIntOrNull() ?: 0 }
                        }
                        val hasPosition = (indices[0].getOrNull(0) ?: 0) != 0
                        val hasUV = (indices[0].getOrNull(1) ?: 0) != 0
                        val hasNormal = (indices[0].getOrNull(2) ?: 0) != 0
                        val hasColor = colors.isNotEmpty()
                        val hasTangents = tangents.isNotEmpty()
                        val hasBitangents = bitangents.isNotEmpty()

                        activeMesh.add(
                            IndexedPolygon(
                                if (hasPosition) indices.map { it[0] - 1 } else listOf(),
                                if (hasUV) indices.map { it[1] - 1 } else listOf(),
                                if (hasColor) indices.map { it[0] - 1 } else listOf(),
                                if (hasNormal) indices.map { it[2] - 1 } else listOf(),
                                if (hasTangents) indices.map { it[2] - 1 } else listOf(),
                                if (hasBitangents) indices.map { it[2] - 1 } else listOf()
                            )
                        )

                        if (meshes.isEmpty()) {
                            meshes["no-name"] = activeMesh
                        }
                    }
                }
            }
        }
    }

    val vertexData = VertexData(positions, textureCoords, colors, normals)
    return CompoundMeshData(
        vertexData,
        meshes.mapValues {
            MeshData(vertexData, it.value)
        }
    )
}

/**
 * Loads a Wavefront OBJ file representation provided as a list of strings and parses it into a VertexBuffer.
 *
 * @param lines The list of strings representing the content of the Wavefront OBJ file. Each string corresponds to a line in the file.
 * @return A VertexBuffer containing the vertex information parsed from the OBJ data.
 */
fun loadOBJasVertexBuffer(lines: List<String>): VertexBuffer {
    return readObjMeshData(lines).toVertexBuffer()
}

/**
 * Parses a list of strings representing the content of a Wavefront OBJ file and converts it into
 * a map of polygon groups, where each key corresponds to a mesh name and the value is a list of polygons.
 *
 * @param lines the lines of the OBJ file as a list of strings, with each string representing a line in the file.
 * @return a map where keys are mesh names and values are lists of polygons ([IPolygon]) corresponding to those meshes.
 */
fun loadOBJ(lines: List<String>): Map<String, List<IPolygon>> = readObjMeshData(lines).triangulate().toPolygons()

