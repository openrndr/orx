package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoRotation
import org.openrndr.ffmpeg.VideoWriterProfile

/**
 * This profile requires specifying a file name like this
 * outputFile = "frame-%05d.png"
 * where `%05d` means "zero-padded five-digit frame number".
 * The frame number format is not optional.
 */
class PNGProfile : VideoWriterProfile() {
    override val fileExtension = "png"

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
 * Configure a png sequence profile
 */
fun ScreenRecorder.pngSequence(configure : PNGProfile.() -> Unit = {}) {
    profile = PNGProfile().apply(configure)
}