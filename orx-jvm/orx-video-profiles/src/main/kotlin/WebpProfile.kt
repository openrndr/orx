package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoRotation
import org.openrndr.ffmpeg.VideoWriterProfile

class WebpProfile : VideoWriterProfile() {
    override val fileExtension = "webp"


    val filters = mutableListOf("vflip")

    /**
     * Rotates the frames, a 90 degree rotation swaps the width and height of the video
     */
    var rotation = VideoRotation.NONE

    override fun arguments(): Array<String> {
        return arrayOf("-vf", (filters + rotation.filters).joinToString(","))
    }
}

/**
 * Configure a webp video profile
 */
fun ScreenRecorder.webp(configure : WebpProfile.() -> Unit) {
    profile = WebpProfile().apply(configure)
}

