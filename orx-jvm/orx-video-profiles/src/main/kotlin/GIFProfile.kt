package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoRotation
import org.openrndr.ffmpeg.VideoWriterProfile

class GIFProfile : VideoWriterProfile() {
    override val fileExtension = "gif"

    val filters = mutableListOf("split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse=dither=none:diff_mode=rectangle", "vflip")

    /**
     * Rotates the frames, a 90 degree rotation swaps the width and height of the video
     */
    var rotation = VideoRotation.NONE

    override fun arguments(): Array<String> {
        return arrayOf("-vf", (filters + rotation.filters).joinToString(","))
    }
}

/**
 * Configure an animated GIF video profile
 */
fun ScreenRecorder.gif(configure : GIFProfile.() -> Unit = {}) {
    profile = GIFProfile().apply(configure)
}