package org.openrndr.extra.videoprofiles
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.ffmpeg.HDRTransfer
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoRotation
import org.openrndr.ffmpeg.VideoWriterProfile

private val logger = KotlinLogging.logger { }

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

    /**
     * Encode colors at full resolution (4:4:4) instead of at half resolution (4:2:0). This keeps the colors of thin
     * lines and edges, but the video does not play in all players. VideoToolbox does not encode 4:4:4, with full
     * chroma libx265 is used instead.
     */
    var fullChroma = false

    /**
     * The encoder, libx265 when VideoToolbox would be used for full chroma
     */
    private fun encoder(): String? = if (fullChroma && isVideoToolbox(videoCodec)) {
        logger.warn { "$videoCodec does not encode full chroma, using $CODEC_LIBX265" }
        CODEC_LIBX265
    } else {
        videoCodec
    }
    var userArguments = emptyArray<String>()

    val filters = mutableListOf("vflip")

    /**
     * Rotates the frames, a 90 degree rotation swaps the width and height of the video
     */
    var rotation = VideoRotation.NONE

    var tagArguments = listOf("-tag:v", "hvc1")

    /**
     * Encode HDR video with this transfer function, or SDR video when null. HDR video is encoded in BT.2020 with 10
     * bits per component, from frames with linear colors in which 1.0 is SDR white (203 nits).
     */
    var hdr = null as HDRTransfer?

    /**
     * The maximum content light level (MaxCLL) in nits, written as metadata of PQ video encoded with libx265
     */
    var maxContentLightLevel = null as Int?

    /**
     * The maximum frame average light level (MaxFALL) in nits, written as metadata of PQ video encoded with libx265
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
        val videoCodec = encoder()
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
        val pixelFormat = when {
            videoToolbox -> "p010le"
            fullChroma -> "yuv444p10le"
            else -> "yuv420p10le"
        }
        val profileArguments = if (videoToolbox) arrayOf("-profile:v", "main10") else emptyArray()
        // the frames are already Y'CbCr, they are only subsampled
        val filterArguments = arrayOf("-vf", (filters + rotation.filters + "format=$pixelFormat").joinToString(","))

        return hwaccelArguments + arrayOf("-pix_fmt", pixelFormat) + hdrColorArguments() + filterArguments +
                videoCodecArguments + profileArguments + qualityArguments(videoCodec) + x265Parameters + tagArguments +
                userArguments
    }

    /**
     * The quality arguments of the encoder: a constant quality for VideoToolbox, a constant rate factor and preset
     * for libx265
     */
    private fun qualityArguments(videoCodec: String?): Array<String> = if (isVideoToolbox(videoCodec)) {
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

        // the filters are not changed, the arguments can be requested more than once
        val filters = filters + rotation.filters +
                (if (highPrecisionChroma) listOf("colorspace=bt709:iall=bt601-6-625:fast=1") else emptyList())

        val videoCodec = encoder()
        val pixelFormat = if (fullChroma) "yuv444p" else pixelFormat
        val hwaccelArguments = hwaccel?.let { arrayOf("-hwaccel", it) } ?: emptyArray()
        val pixelFormatArguments = pixelFormat?.let { arrayOf("-pix_fmt", it) } ?: emptyArray()
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        val filterArguments = arrayOf("-vf", filters.joinToString(","))

        val arguments =
            hwaccelArguments + pixelFormatArguments + chromaArguments + filterArguments + videoCodecArguments + qualityArguments(videoCodec) + tagArguments + userArguments

        return arguments
    }
}

/**
 * Configure a h265 video profile
 */
fun ScreenRecorder.h265(configure : H265Profile.() -> Unit = {}) {
    profile = H265Profile().apply(configure)
}
