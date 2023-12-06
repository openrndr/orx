package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile

@Deprecated("use h265 profile", replaceWith = ReplaceWith("H265Profile"))
typealias X265Profile = H265Profile


class H265Profile : VideoWriterProfile() {

    /**
     * constant rate factor (default is 23)
     */
    var constantRateFactor = null as Int?

    @Deprecated("Use constantRateFactor property")
    fun constantRateFactor(factor: Int) {
        constantRateFactor = factor
    }

    override var fileExtension = "mp4"

    var highPrecisionChroma = true

    val CODEC_LIBX265 = "libx265"


    var videoCodec = CODEC_LIBX265 as String?
    var hwaccel = null as String?
    var preset = null as String?


    var pixelFormat = "yuv420p" as String?
    var userArguments = emptyArray<String>()

    val filters = mutableListOf("vflip")

    var tagArguments = listOf("-tag:v", "hvc1")

    override fun arguments(): Array<String> {
        val chromaArguments = if (highPrecisionChroma) {
            arrayOf(
                "-sws_flags", "spline+accurate_rnd+full_chroma_int",
                "-color_range", "1",
                "-colorspace", "1",
                "-color_primaries","1",
                "-color_trc", "1"
            )
        } else {
            emptyArray()
        }

        if (highPrecisionChroma) {
            filters.add("colorspace=bt709:iall=bt601-6-625:fast=1")
        }

        val hwaccelArguments = hwaccel?.let { arrayOf("-hwaccel", it) } ?: emptyArray()
        val pixelFormatArguments = pixelFormat?.let { arrayOf("-pix_fmt", it) } ?: emptyArray()
        val constantRateArguments = constantRateFactor?.let { arrayOf("-crf", it.toString()) } ?: emptyArray()
        val presetArguments = preset?.let { arrayOf("-preset", it) } ?: emptyArray()
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        val filterArguments = arrayOf("-vf", filters.joinToString(","))

        val arguments =
            hwaccelArguments + pixelFormatArguments + chromaArguments + filterArguments + videoCodecArguments + constantRateArguments + presetArguments + tagArguments + userArguments

        return arguments
    }
}

/**
 * Configure a h265 video profile
 */
fun ScreenRecorder.h265(configure : H265Profile.() -> Unit = {}) {
    profile = H265Profile().apply(configure)
}
