package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile

@Deprecated("use h265 profile", replaceWith = ReplaceWith("H265Profile"))
typealias X265Profile = H265Profile

class H265Profile : VideoWriterProfile() {
    internal var mode = WriterMode.Normal
    internal var constantRateFactor = 28
    var hlg = false

    enum class WriterMode {
        Normal,
        Lossless
    }

    fun mode(mode: WriterMode): X265Profile {
        this.mode = mode
        return this
    }

    /**
     * Sets the constant rate factor
     * @param constantRateFactor the constant rate factor (default is 28)
     * @return
     */
    fun constantRateFactor(constantRateFactor: Int): X265Profile {
        this.constantRateFactor = constantRateFactor
        return this
    }

    override val fileExtension = "mp4"


    override fun arguments(): Array<String> {
        when (mode) {
            WriterMode.Normal -> {
                return if (!hlg) {
                    arrayOf("-pix_fmt", "yuv420p", // this will produce videos that are playable by quicktime
                            "-vf", "vflip",
                            "-an", "-vcodec", "libx265", "-crf", "" + constantRateFactor)
                } else {
                    arrayOf( // this will produce videos that are playable by quicktime
                            "-an", "" +
                            "-vcodec", "libx265",
                            "-pix_fmt", "yuv420p10le",
                            "-color_primaries", "bt2020",
                            "-colorspace", "bt2020_ncl",
                            "-color_trc", "arib-std-b67",
                            "-crf", "" + constantRateFactor)
                    // transfer=arib-std-b67
                }
            }
            WriterMode.Lossless -> {
                return arrayOf("-pix_fmt", "yuv420p10", // this will produce videos that are playable by quicktime
                        "-an", "-vcodec", "libx265", "-preset", "ultrafast")
            }
            else -> {
                throw RuntimeException("unsupported write mode")
            }
        }
    }
}

/**
 * Configure a h265 video profile
 */
fun ScreenRecorder.h265(configure : H265Profile.() -> Unit = {}) {
    profile = H265Profile().apply(configure)
}
