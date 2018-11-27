package org.openrndr.extra.objloader

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class Triangle(val positions: Array<Vector3> = emptyArray(),
               val normals: Array<Vector3> = emptyArray(),
               val textureCoords: Array<Vector2> = emptyArray()) {
    fun transform(t: Matrix44): Triangle {
        return Triangle(positions.map { (t * it.xyz1).div }.toTypedArray(), normals, textureCoords)
    }
}

class Box(val corner: Vector3, val width: Double, val height: Double, val depth: Double)

fun bounds(triangles: List<Triangle>): Box {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY

    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    var maxZ = Double.NEGATIVE_INFINITY

    triangles.forEach {
        it.positions.forEach {
            minX = Math.min(minX, it.x)
            minY = Math.min(minY, it.y)
            minZ = Math.min(minZ, it.z)

            maxX = Math.max(maxX, it.x)
            maxY = Math.max(maxY, it.y)
            maxZ = Math.max(maxZ, it.z)
        }
    }
    return Box(Vector3(minX, minY, minZ), maxX - minX, maxY - minY, maxZ - minZ)
}

fun List<Triangle>.vertexBuffer():VertexBuffer {
    val vertexBuffer = vertexBuffer(objVertexFormat, size*3)
    vertexBuffer.put {
        this@vertexBuffer.forEach {
            for (i in 0 until it.positions.size) {
                write(it.positions[i])
                write(it.normals[i])
                write(Vector2.ZERO)
            }
        }
    }
    vertexBuffer.shadow.destroy()
    return vertexBuffer
}

fun loadOBJ(fileOrUrl: String): Map<String, List<Triangle>> {
    return try {
        val url = URL(fileOrUrl)
        loadOBJ(url)
    } catch (e: MalformedURLException) {
        loadOBJ(File(fileOrUrl))
    }
}

private val objVertexFormat = vertexFormat {
    position(3)
    normal(3)
    textureCoordinate(2)
}

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
    val objects = loadOBJ(lines)
    val triangleCount = objects.values.sumBy { it.size }
    val vertexBuffer = vertexBuffer(objVertexFormat, triangleCount * 3)

    vertexBuffer.put {
        objects.entries.forEach {
            it.value.forEach {
                for (i in 0 until it.positions.size) {
                    write(it.positions[i])
                    if (it.normals.size > 0) {
                        write(it.normals[i])
                    } else {
                        val d0 = it.positions[2] - it.positions[0]
                        val d1 = it.positions[1] - it.positions[0]
                        write(d0.normalized.cross(d1.normalized).normalized)
                    }
                    if (it.textureCoords.size > 0) {
                        write(it.textureCoords[i])
                    } else {
                        write(Vector2.ZERO)
                    }
                }
            }
        }
    }

    vertexBuffer.shadow.destroy()
    return vertexBuffer
}

fun loadOBJ(file: File): Map<String, List<Triangle>> = loadOBJ(file.readLines())
fun loadOBJ(url: URL): Map<String, List<Triangle>> = loadOBJ(url.readText().split("\n"))

fun loadOBJ(lines: List<String>): Map<String, List<Triangle>> {
    val meshes = mutableMapOf<String, List<Triangle>>()
    val positions = mutableListOf<Vector3>()
    val normals = mutableListOf<Vector3>()
    val textureCoords = mutableListOf<Vector2>()
    var activeMesh = mutableListOf<Triangle>()

    lines.forEach { line ->
        if (line.isNotEmpty()) {
            val tokens = line.split(Regex("[ |\t]+")).map { it.trim() }.filter { it.isNotEmpty() }

            if (tokens.isNotEmpty()) {
                when (tokens[0]) {
                    "v" -> {
                        positions += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                    }
                    "vn" -> normals += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                    "vt" -> textureCoords += Vector2(tokens[1].toDouble(), tokens[2].toDouble())
                    "g" -> {
                        activeMesh = mutableListOf()
                        meshes[tokens[1]] = activeMesh
                    }
                    "f" -> {
                        val indices = tokens.subList(1, tokens.size).map { it.split("/") }.map {
                            it.map { it.toIntOrNull() }
                        }

                        if (indices.size == 3) {

                            val attributes = indices[0].size

                            val ps = if (attributes >= 1) arrayOf(
                                    indices[0][0]?.let { positions[it - 1] } ?: Vector3.ZERO,
                                    indices[1][0]?.let { positions[it - 1] } ?: Vector3.ZERO,
                                    indices[2][0]?.let { positions[it - 1] } ?: Vector3.ZERO)
                            else
                                emptyArray()

                            val tcs = if (attributes >= 2) arrayOf(
                                    indices[0][1]?.let { textureCoords[it - 1] } ?: Vector2.ZERO,
                                    indices[1][1]?.let { textureCoords[it - 1] } ?: Vector2.ZERO,
                                    indices[2][1]?.let { textureCoords[it - 1] } ?: Vector2.ZERO)
                            else
                                emptyArray()


                            val ns = if (attributes >= 3) arrayOf(
                                    indices[0][2]?.let { normals[it - 1] } ?: Vector3.ZERO,
                                    indices[1][2]?.let { normals[it - 1] } ?: Vector3.ZERO,
                                    indices[2][2]?.let { normals[it - 1] } ?: Vector3.ZERO)
                            else
                                emptyArray()

                            activeMesh.add(Triangle(ps, ns, tcs))
                            if(meshes.isEmpty()) {
                                meshes["no-name"] = activeMesh
                            }
                        } else {
                            TODO("implement non triangular surfaces ${indices.size}")
                        }
                    }
                }
            }
        }
    }
    return meshes
}