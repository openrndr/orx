package org.openrndr.extra.runway

import com.google.gson.Gson
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.FileFormat
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Construct a base64 representation of an encoded image
 */
fun ColorBuffer.toData(format: FileFormat = FileFormat.JPG): String {
    val tempFile = File.createTempFile("orx-runway", null)
    saveToFile(tempFile, format, async = false)
    val ref = File(tempFile.absolutePath)
    val imageBytes = ref.readBytes()
    val encoder = Base64.getEncoder()
    val base64Data = encoder.encodeToString(imageBytes)
    tempFile.delete()
    return "data:image/jpeg;base64,$base64Data"
}

/**
 * Construct a color buffer from a base64 data string
 */
fun ColorBuffer.Companion.fromData(data: String): ColorBuffer {
    val decoder = Base64.getDecoder()
    val commaIndex = data.indexOf(",")
    val imageData = decoder.decode(data.drop(commaIndex+1))

    ByteArrayInputStream(imageData).use {
        return ColorBuffer.fromStream(it)
    }
}

/**
 * Perform a Runway query
 * @param target url string e.g. http://localhost:8000/query
 */
inline fun <Q, reified R> runwayQuery(target: String, query: Q): R {
    val queryJson = Gson().toJson(query)
    val connection = URL(target).openConnection() as HttpURLConnection
    with(connection) {
        doOutput = true
        connectTimeout = 1_000
        readTimeout = 200_000
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
    }

    val outputStream = connection.outputStream
    outputStream.write(queryJson.toByteArray())
    outputStream.flush()

    val inputStream = connection.inputStream
    val responseJson = String(inputStream.readBytes())
    inputStream.close()
    connection.disconnect()
    return Gson().fromJson(responseJson, R::class.java)
}