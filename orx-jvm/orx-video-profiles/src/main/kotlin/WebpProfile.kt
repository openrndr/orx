package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.VideoWriterProfile

class WebpProfile : VideoWriterProfile() {
    override val fileExtension = "webp"

    override fun arguments(): Array<String> {
        return arrayOf("-vf", "vflip")
    }
}
