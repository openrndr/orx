package org.openrndr.extra.dnk3.gltf

import com.google.gson.Gson
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
        return chunkBuffer
    }

    val jsonBuffer = readChunk()
    jsonBuffer.rewind()
    val jsonByteArray = ByteArray(jsonBuffer.capacity())
    jsonBuffer.get(jsonByteArray)
    val json = String(jsonByteArray)
    val gson = Gson()
    val bufferBuffer = if (channel.position() < length) readChunk() else null

    return gson.fromJson(json, GltfFile::class.java).apply {
        this.file = file
        this.bufferBuffer = bufferBuffer
    }
}