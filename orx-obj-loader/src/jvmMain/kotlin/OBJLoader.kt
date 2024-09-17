package org.openrndr.extra.mesh

import org.openrndr.draw.VertexBuffer
import java.io.File
import java.net.MalformedURLException
import java.net.URL

/**
 * Loads an OBJ file as a Map of names to lists of [Polygon].
 * Use this method to access the loaded OBJ data from the CPU.
 */
fun loadOBJ(fileOrUrl: String): Map<String, List<IPolygon>> {
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

fun loadOBJ(file: File) = loadOBJ(file.readLines())
fun loadOBJEx(file: File) = readObjMeshData(file.readLines())
fun loadOBJ(url: URL) = loadOBJ(url.readText().split("\n"))
fun loadOBJEx(url: URL) = readObjMeshData(url.readText().split("\n"))



fun loadOBJMeshData(file: File) = readObjMeshData(file.readLines())
