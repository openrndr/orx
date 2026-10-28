package org.openrndr.extra.videoprofiles
import org.openrndr.ffmpeg.HDRTransfer
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile

@Deprecated("use h265 profile", replaceWith = ReplaceWith("H265Profile"))
typealias X265Profile = H265Profile


class H265Profile : VideoWriterProfile() {

    /**
     * constant rate factor (default is 23), used by libx265
     */
    var constantRateFactor = null as Int?

    @Deprecated("Use constantRateFactor property")
    fun constantRateFactor(factor: Int) {
        constantRateFactor = factor
    }

    override var fileExtension = "mp4"

    /**
     * Convert SDR video with high precision chroma, not used for HDR video
     */
    var highPrecisionChroma = true

    val CODEC_LIBX265 = "libx265"

    /**
     * The hardware HEVC encoder of macOS
     */
    val CODEC_HEVC_VIDEOTOOLBOX = "hevc_videotoolbox"

    var videoCodec = CODEC_LIBX265 as String?
    var hwaccel = null as String?

    /**
     * The encoding preset of libx265
     */
    var preset = null as String?

    /**
     * Constant quality of VideoToolbox encoding, from 1 to 100 where higher is better, or null for VideoToolbox's
     * default bit rate. The default of 55 is comparable to the default quality of libx265, at about twice the size.
     */
    var quality = 55 as Int?

    /**
     * Encode with the hardware HEVC encoder where one is supported: VideoToolbox on macOS. Encoding falls back to
     * software when the hardware does not support HEVC. On other platforms the encoder is not changed.
     */
    fun hardwareAcceleration() {
        if (isMacOS) {
            videoCodec = CODEC_HEVC_VIDEOTOOLBOX
        }
    }


    /**
     * The pixel format of SDR video, HDR video is encoded with 10 bits per component (yuv420p10le)
     */
    var pixelFormat = "yuv420p" as String?
    var userArguments = emptyArray<String>()

    val filters = mutableListOf("vflip")

    var tagArguments = listOf("-tag:v", "hvc1")

    /**
     * Encode HDR video with this transfer function, or SDR video when null. HDR video is encoded in BT.2020 with 10
     * bits per component, from frames with linear colors in which 1.0 is SDR white (203 nits).
     */
    var hdr = null as HDRTransfer?

    /**
     * The maximum content light level (MaxCLL) in nits, written as metadata of PQ video
     */
    var maxContentLightLevel = null as Int?

    /**
     * The maximum frame average light level (MaxFALL) in nits, written as metadata of PQ video
     */
    var maxFrameAverageLightLevel = null as Int?

    override val hdrTransfer: HDRTransfer? get() = hdr

    private val hdrTransferName
        get() = when (hdr) {
            HDRTransfer.PQ -> "smpte2084"
            HDRTransfer.HLG -> "arib-std-b67"
            null -> error("not an HDR profile")
        }

    private fun hdrColorArguments() = arrayOf(
        "-color_primaries", "bt2020", "-color_trc", hdrTransferName, "-colorspace", "bt2020nc", "-color_range", "tv"
    )

    /**
     * HDR frames are Y'CbCr encoded like the output, tagging them keeps FFmpeg from converting them
     */
    override fun inputArguments(): Array<String> = if (hdr != null) hdrColorArguments() else emptyArray()

    private fun hdrArguments(): Array<String> {
        val x265Parameters = if (videoCodec == CODEC_LIBX265) {
            val parameters = mutableListOf("colorprim=bt2020", "transfer=$hdrTransferName", "colormatrix=bt2020nc", "range=limited")
            if (hdr == HDRTransfer.PQ) {
                parameters.add("hdr10=1")
                if (maxContentLightLevel != null || maxFrameAverageLightLevel != null) {
                    parameters.add("max-cll=${maxContentLightLevel ?: 0},${maxFrameAverageLightLevel ?: 0}")
                }
            }
            arrayOf("-x265-params", parameters.joinToString(":"))
        } else {
            emptyArray()
        }

        val hwaccelArguments = hwaccel?.let { arrayOf("-hwaccel", it) } ?: emptyArray()
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        // VideoToolbox takes 10 bit frames as p010le and encodes them with the Main 10 profile
        val videoToolbox = isVideoToolbox(videoCodec)
        val pixelFormat = if (videoToolbox) "p010le" else "yuv420p10le"
        val profileArguments = if (videoToolbox) arrayOf("-profile:v", "main10") else emptyArray()
        // the frames are already Y'CbCr, they are only subsampled
        val filterArguments = arrayOf("-vf", (filters + "format=$pixelFormat").joinToString(","))

        return hwaccelArguments + arrayOf("-pix_fmt", pixelFormat) + hdrColorArguments() + filterArguments +
                videoCodecArguments + profileArguments + qualityArguments() + x265Parameters + tagArguments +
                userArguments
    }

    /**
     * The quality arguments of the encoder: a constant quality for VideoToolbox, a constant rate factor and preset
     * for libx265
     */
    private fun qualityArguments(): Array<String> = if (isVideoToolbox(videoCodec)) {
        videoToolboxArguments + (quality?.let { arrayOf("-q:v", it.toString()) } ?: emptyArray())
    } else {
        (constantRateFactor?.let { arrayOf("-crf", it.toString()) } ?: emptyArray()) +
                (preset?.let { arrayOf("-preset", it) } ?: emptyArray())
    }

    override fun arguments(): Array<String> {
        if (hdr != null) {
            return hdrArguments()
        }

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
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        val filterArguments = arrayOf("-vf", filters.joinToString(","))

        val arguments =
            hwaccelArguments + pixelFormatArguments + chromaArguments + filterArguments + videoCodecArguments + qualityArguments() + tagArguments + userArguments

        return arguments
    }
}

/**
 * Configure a h265 video profile
 */
fun ScreenRecorder.h265(configure : H265Profile.() -> Unit = {}) {
    profile = H265Profile().apply(configure)
}
