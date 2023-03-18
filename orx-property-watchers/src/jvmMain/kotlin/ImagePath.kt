package org.openrndr.extra.propertywatchers

import org.openrndr.Program
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadImage
import java.io.File
import kotlin.reflect.KProperty0

fun Program.watchingImagePath(pathProperty: KProperty0<String>, imageTransform: (ColorBuffer) -> ColorBuffer = { it }) =
    watchingProperty(pathProperty, cleaner = { it.destroy() }) {
        val file = File(it)
        require(file.exists()) { "$it does not exist" }
        require(file.isFile) { "$it is not a file" }
        val image = loadImage(file)
        val transformedImage = imageTransform(image)
        if (image != transformedImage) {
            image.destroy()
        }
        transformedImage
    }
