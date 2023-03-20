package org.openrndr.extra.propertywatchers

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.events.Event
import java.io.File
import kotlin.reflect.KProperty0

/**
 * Delegate property value by watching a path property
 * @param pathProperty the property holding a path to watch
 * @param imageTransform an optional image transform function
 * @since 0.4.3
 */
fun watchingImagePath(
    pathProperty: KProperty0<String>,
    imageChangedEvent: Event<ColorBuffer>? = null,
    imageTransform: (ColorBuffer) -> ColorBuffer = { it }
) =
    watchingProperty(pathProperty, imageChangedEvent, cleaner = { it.destroy() }) {
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