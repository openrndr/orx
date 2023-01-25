package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile

class WebpProfile : VideoWriterProfile() {
    override val fileExtension = "webp"


    val filters = mutableListOf("vflip")

    override fun arguments(): Array<String> {
        return arrayOf("-vf", filters.joinToString(","))
    }
}

/**
 * Configure a webp video profile
 */
fun ScreenRecorder.webp(configure : WebpProfile.() -> Unit) {
    profile = WebpProfile().apply(configure)
}

