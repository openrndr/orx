package org.openrndr.extra.objloader

import org.openrndr.draw.VertexBuffer
import org.openrndr.math.*
import java.io.File
import java.net.MalformedURLException
import java.net.URL

/**
 * Loads an OBJ file as a Map of names to lists of [Polygon].
 * Use this method to access the loaded OBJ data from the CPU.
 */
fun loadOBJ(fileOrUrl: String): Map<String, List<Polygon>> {
    return try {
        val url = URL(fileOrUrl)
        loadOBJ(url)
    } catch (e: MalformedURLException) {
        loadOBJ(File(fileOrUrl))
    }
}

/**
 * Loads an OBJ file as a [VertexBuffer].
 * Use this method to render / process the loaded OBJ data using the GPU.
 */
fun loadOBJasVertexBuffer(fileOrUrl: String): VertexBuffer {
    return try {
        val url = URL(fileOrUrl)
        loadOBJasVertexBuffer(url)
    } catch (e: MalformedURLException) {
        loadOBJasVertexBuffer(File(fileOrUrl))
    }
}

fun loadOBJasVertexBuffer(url: URL): VertexBuffer = loadOBJasVertexBuffer(url.readText().split("\n"))
fun loadOBJasVertexBuffer(file: File): VertexBuffer = loadOBJasVertexBuffer(file.readLines())
fun loadOBJasVertexBuffer(lines: List<String>): VertexBuffer {
    return loadOBJMeshData(lines).toVertexBuffer()
}

fun loadOBJ(file: File) = loadOBJ(file.readLines())
fun loadOBJEx(file: File) = loadOBJMeshData(file.readLines())
fun loadOBJ(url: URL) = loadOBJ(url.readText().split("\n"))
fun loadOBJEx(url: URL) = loadOBJMeshData(url.readText().split("\n"))

fun loadOBJ(lines: List<String>): Map<String, List<Polygon>> = loadOBJMeshData(lines).triangulate().flattenPolygons()


fun loadOBJMeshData(file: File) = loadOBJMeshData(file.readLines())
fun loadOBJMeshData(lines: List<String>): MeshData {
    val meshes = mutableMapOf<String, List<IndexedPolygon>>()
    val positions = mutableListOf<Vector3>()
    val normals = mutableListOf<Vector3>()
    val textureCoords = mutableListOf<Vector2>()
    var activeMesh = mutableListOf<IndexedPolygon>()

    lines.forEach { line ->
        if (line.isNotEmpty()) {
            val tokens = line.split(Regex("[ |\t]+")).map { it.trim() }.filter { it.isNotEmpty() }

            if (tokens.isNotEmpty()) {
                when (tokens[0]) {
                    "v" -> positions += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                    "vn" -> normals += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
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

                        activeMesh.add(
                            IndexedPolygon(
                                if (hasPosition) indices.map { it[0] - 1 }.toIntArray() else intArrayOf(),
                                if (hasUV) indices.map { it[1] - 1 }.toIntArray() else intArrayOf(),
                                if (hasNormal) indices.map { it[2] - 1 }.toIntArray() else intArrayOf()
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

    return MeshData(
        VertexData(positions.toTypedArray(), normals.toTypedArray(), textureCoords.toTypedArray()),
        meshes
    )
}
