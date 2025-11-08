package org.openrndr.extra.dnk3.gltf

import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun loadGltfFromGlbFile(file: File): GltfFile {
    val channel = RandomAccessFile(file, "r").channel
    val headerBuffer = ByteBuffer.allocate(12).order(ByteOrder.nativeOrder())

    headerBuffer.rewind()
    channel.read(headerBuffer)
    headerBuffer.rewind()

    val magic = headerBuffer.int
    val version = headerBuffer.int
    val length = headerBuffer.int

    fun readChunk(): ByteBuffer {
        val chunkHeader = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder())
        channel.read(chunkHeader)
        chunkHeader.rewind()
        val chunkLength = chunkHeader.int
        val chunkType = chunkHeader.int
        val chunkBuffer =
                if (chunkType == 0x004E4942) ByteBuffer.allocateDirect(chunkLength) else ByteBuffer.allocate(chunkLength)
        (chunkBuffer as ByteBuffer)
        channel.read(chunkBuffer)
        chunkBuffer.order(ByteOrder.nativeOrder())
        return chunkBuffer
    }

    val jsonBuffer = readChunk()
    jsonBuffer.rewind()
    val jsonByteArray = ByteArray(jsonBuffer.capacity())
    jsonBuffer.get(jsonByteArray)
    val json = String(jsonByteArray)
    val bufferBuffer = if (channel.position() < length) readChunk() else null

    val gltFile = Json { ignoreUnknownKeys = true }.decodeFromString<GltfFile>(json)
    gltFile.file = file
    gltFile.bufferBuffer = bufferBuffer

    return gltFile
}