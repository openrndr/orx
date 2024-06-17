package org.openrndr.extra.objloader

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import kotlin.math.max
import kotlin.math.min

/**
 * A 3D Triangle
 *
 * @property positions Three vertex positions
 * @property normals Three vertex normals
 * @property textureCoords There texture coordinates
 * @constructor Create empty Triangle
 */
class Triangle(
    val positions: Array<Vector3> = emptyArray(),
    val normals: Array<Vector3> = emptyArray(),
    val textureCoords: Array<Vector2> = emptyArray()
) {
    fun transform(t: Matrix44): Triangle {
        return Triangle(positions.map { (t * it.xyz1).div }.toTypedArray(), normals, textureCoords)
    }
}

class Box(val corner: Vector3, val width: Double, val height: Double, val depth: Double)

/**
 * Calculates the bounding box of a list of [Triangle].
 */
fun bounds(triangles: List<Triangle>): Box {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY

    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    var maxZ = Double.NEGATIVE_INFINITY

    triangles.forEach {
        it.positions.forEach {
            minX = min(minX, it.x)
            minY = min(minY, it.y)
            minZ = min(minZ, it.z)

            maxX = max(maxX, it.x)
            maxY = max(maxY, it.y)
            maxZ = max(maxZ, it.z)
        }
    }
    return Box(Vector3(minX, minY, minZ), maxX - minX, maxY - minY, maxZ - minZ)
}

/**
 * The vertexFormat of a mesh including positions, normals and texture coordinates.
 */
private val objVertexFormat = vertexFormat {
    position(3)
    normal(3)
    textureCoordinate(2)
}
/**
 * Converts a list of [Triangle] into a [VertexBuffer]
 */
fun List<Triangle>.vertexBuffer(): VertexBuffer {
    val vertexBuffer = vertexBuffer(objVertexFormat, size * 3)
    vertexBuffer.put {
        this@vertexBuffer.forEach {
            for (i in it.positions.indices) {
                write(it.positions[i])
                write(it.normals[i])
                write(it.textureCoords[i])
            }
        }
    }
    vertexBuffer.shadow.destroy()
    return vertexBuffer
}

/**
 * Loads an OBJ file
 *
 * @param fileOrUrl
 * @return
 */
fun loadOBJ(fileOrUrl: String): Map<String, List<Triangle>> {
    return try {
        val url = URL(fileOrUrl)
        loadOBJ(url)
    } catch (e: MalformedURLException) {
        loadOBJ(File(fileOrUrl))
    }
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
    val triangleCount = objects.values.sumOf { it.size }
    val vertexBuffer = vertexBuffer(objVertexFormat, triangleCount * 3)

    vertexBuffer.put {
        objects.entries.forEach {
            it.value.forEach { tri ->
                for (i in tri.positions.indices) {
                    write(tri.positions[i])
                    if (tri.normals.isNotEmpty()) {
                        write(tri.normals[i])
                    } else {
                        val d0 = tri.positions[2] - tri.positions[0]
                        val d1 = tri.positions[1] - tri.positions[0]
                        write(d0.normalized.cross(d1.normalized).normalized)
                    }
                    if (tri.textureCoords.isNotEmpty()) {
                        write(tri.textureCoords[i])
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

fun loadOBJ(file: File) = loadOBJ(file.readLines())
fun loadOBJEx(file: File) = loadOBJEx(file.readLines())
fun loadOBJ(url: URL) = loadOBJ(url.readText().split("\n"))
fun loadOBJEx(url: URL) = loadOBJEx(url.readText().split("\n"))

class OBJData(val positions: List<Vector3>, val normals: List<Vector3>, val textureCoords: List<Vector2>)

fun loadOBJ(lines: List<String>): Map<String, List<Triangle>> = loadOBJEx(lines).second

fun loadOBJEx(lines: List<String>): Pair<OBJData, Map<String, List<Triangle>>> {
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
                    "v" -> positions += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                    "vn" -> normals += Vector3(tokens[1].toDouble(), tokens[2].toDouble(), tokens[3].toDouble())
                    "vt" -> textureCoords += Vector2(tokens[1].toDouble(), tokens[2].toDouble())
                    "g" -> {
                        activeMesh = mutableListOf()
                        meshes[tokens.getOrNull(1) ?: "no-name-${meshes.size}"] = activeMesh
                    }

                    "f" -> {
                        val indices = tokens.subList(1, tokens.size).map { it.split("/") }.map {
                            it.map { it.toIntOrNull() }
                        }

                        for (i in 0 until indices.size - 2) {

                            val attributes = indices[0].size
                            val o = i * 2
                            val s = indices.size

                            val ps = if (attributes >= 1) arrayOf(
                                indices[(0 + o) % s][0]?.let { positions[it - 1] } ?: Vector3.ZERO,
                                indices[(1 + o) % s][0]?.let { positions[it - 1] } ?: Vector3.ZERO,
                                indices[(2 + o) % s][0]?.let { positions[it - 1] } ?: Vector3.ZERO)
                            else
                                emptyArray()

                            val tcs = if (attributes >= 2) arrayOf(
                                indices[(0 + o) % s][1]?.let { textureCoords[it - 1] } ?: Vector2.ZERO,
                                indices[(1 + o) % s][1]?.let { textureCoords[it - 1] } ?: Vector2.ZERO,
                                indices[(2 + o) % s][1]?.let { textureCoords[it - 1] } ?: Vector2.ZERO)
                            else
                                emptyArray()


                            val ns = if (attributes >= 3) arrayOf(
                                indices[(0 + o) % s][2]?.let { normals[it - 1] } ?: Vector3.ZERO,
                                indices[(1 + o) % s][2]?.let { normals[it - 1] } ?: Vector3.ZERO,
                                indices[(2 + o) % s][2]?.let { normals[it - 1] } ?: Vector3.ZERO)
                            else
                                emptyArray()

                            activeMesh.add(Triangle(ps, ns, tcs))
                            if (meshes.isEmpty()) {
                                meshes["no-name"] = activeMesh
                            }
                        }
                    }
                }
            }
        }
    }
    return Pair(OBJData(positions, normals, textureCoords), meshes)
}
