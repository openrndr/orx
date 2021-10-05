package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.VideoWriterProfile

/**
 * This profile requires specifying a file name like this
 * outputFile = "frame-%05d.png"
 * where `%05d` means "zero-padded five-digit frame number".
 * The frame number format is not optional.
 */
class PNGProfile : VideoWriterProfile() {
    override val fileExtension = "png"

    override fun arguments(): Array<String> {
        return arrayOf("-vf", "vflip")
    }
}