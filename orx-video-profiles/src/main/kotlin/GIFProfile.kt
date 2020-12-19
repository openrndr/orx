package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.VideoWriterProfile

class GIFProfile : VideoWriterProfile() {
    override val fileExtension = "gif"

    override fun arguments(): Array<String> {
        return arrayOf("-vf", "split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse=dither=none:diff_mode=rectangle,vflip")
    }
}
