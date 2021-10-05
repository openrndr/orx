package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.VideoWriterProfile

/**
 * This profile requires specifying a file name like this
 * outputFile = "frame-%05d.tif"
 * where `%05d` means "zero-padded five-digit frame number".
 * The frame number format is not optional.
 */
class TIFFProfile : VideoWriterProfile() {
    override val fileExtension = "tif"

    override fun arguments(): Array<String> {
        return arrayOf("-vf", "vflip")
    }
}
