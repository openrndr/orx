package org.openrndr.extra.videoprofiles
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.ffmpeg.EncoderInput
import org.openrndr.ffmpeg.HDRTransfer
import org.openrndr.ffmpeg.HardwareEncoder
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoRotation
import org.openrndr.ffmpeg.VideoWriterProfile
import org.openrndr.ffmpeg.findHardwareEncoder

private val logger = KotlinLogging.logger { }

@Deprecated("use h265 profile", replaceWith = ReplaceWith("H265Profile"))
typealias X265Profile = H265Profile


class H265Profile : VideoWriterProfile() {

    /**
     * constant rate factor (default is 28), used by libx265
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

    /**
     * The hardware HEVC encoder of NVIDIA GPUs
     */
    val CODEC_HEVC_NVENC = "hevc_nvenc"

    /**
     * The hardware HEVC encoder of Intel and AMD GPUs on Linux
     */
    val CODEC_HEVC_VAAPI = "hevc_vaapi"

    /**
     * The hardware HEVC encoder of Intel GPUs on Windows
     */
    val CODEC_HEVC_QSV = "hevc_qsv"

    /**
     * The hardware HEVC encoder of AMD GPUs on Windows
     */
    val CODEC_HEVC_AMF = "hevc_amf"

    /**
     * The hardware HEVC encoder of Media Foundation on Windows
     */
    val CODEC_HEVC_MF = "hevc_mf"

    var videoCodec = CODEC_LIBX265 as String?

    @Deprecated("-hwaccel selects a decoder, it does not accelerate encoding, use hardwareAcceleration()")
    var hwaccel = null as String?

    /**
     * The encoding preset of libx265
     */
    var preset = null as String?

    /**
     * Constant quality of the hardware encoders, from 1 to 100 where higher is better, or null for their default bit
     * rate. The default of 55 is comparable to the default quality of libx265, at about twice the size.
     */
    var quality = 55 as Int?

    /**
     * The DRM render node that the VAAPI encoder encodes with, set by [hardwareAcceleration]
     */
    var vaapiDevice = HardwareEncoder.DEFAULT_VAAPI_DEVICE

    /**
     * Encode with a hardware HEVC encoder, which uses a fraction of the CPU time of libx265: VideoToolbox on macOS,
     * on Linux NVENC on NVIDIA GPUs or VAAPI on Intel and AMD GPUs, and on Windows NVENC on NVIDIA GPUs, Quick Sync
     * on Intel GPUs, AMF on AMD GPUs or else the hardware encoder of Media Foundation. On Linux and Windows the
     * encoders are tried by encoding a frame like the frames of this profile, see [findHardwareEncoder], libx265 is
     * kept when none of them works. Set [hdr] and [fullChroma] before calling this.
     */
    fun hardwareAcceleration() {
        findHardwareEncoder("hevc", hardwarePixelFormat())?.let {
            videoCodec = it.codec
            it.device?.let { device -> vaapiDevice = device }
        }
    }

    /**
     * The pixel format the hardware encoders encode the frames of this profile in. 10 bit HDR frames are encoded as
     * P010, or with 16 bits per component with full chroma (yuv444p16le, the 10 bit format is not supported by
     * NVENC).
     */
    private fun hardwarePixelFormat() = when {
        hdr != null -> if (fullChroma) "yuv444p16le" else "p010le"
        fullChroma -> "yuv444p"
        else -> pixelFormat ?: "yuv420p"
    }

    /**
     * The pixel format of SDR video, HDR video is encoded with 10 bits per component (yuv420p10le)
     */
    var pixelFormat = "yuv420p" as String?

    /**
     * Encode colors at full resolution (4:4:4) instead of at half resolution (4:2:0). This keeps the colors of thin
     * lines and edges, but the video does not play in all players. Of the hardware encoders only NVENC encodes 4:4:4,
     * with full chroma libx265 is used instead of the others.
     */
    var fullChroma = false

    /**
     * The hardware encoder of [videoCodec], or null for libx265 and for the hardware encoders that do not encode
     * full chroma when [fullChroma] is set
     */
    private fun hardwareEncoder(): HardwareEncoder? {
        val encoder = videoCodec?.let { HardwareEncoder.forCodec(it, vaapiDevice) } ?: return null
        return if (fullChroma && !encoder.encodesFullChroma) {
            logger.warn { "$videoCodec does not encode full chroma, using $CODEC_LIBX265" }
            null
        } else {
            encoder
        }
    }

    /**
     * The encoder, libx265 when a hardware encoder would be used for full chroma
     */
    private fun encoder(hardwareEncoder: HardwareEncoder?): String? =
        if (hardwareEncoder == null && videoCodec?.let { HardwareEncoder.forCodec(it) } != null) CODEC_LIBX265 else videoCodec

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
        val hardwareEncoder = hardwareEncoder()
        val videoCodec = encoder(hardwareEncoder)
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

        @Suppress("DEPRECATION")
        val hwaccelArguments = hwaccel?.let { arrayOf("-hwaccel", it) } ?: emptyArray()
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        // the hardware encoders take 10 bit frames as P010 and encode them with the Main 10 profile, libx265 takes
        // them as yuv420p10le
        val input = hardwareEncoder?.input(hardwarePixelFormat())
            ?: EncoderInput(if (fullChroma) "yuv444p10le" else "yuv420p10le", emptyList())
        val pixelFormatArguments = input.pixelFormat?.let { arrayOf("-pix_fmt", it) } ?: emptyArray()
        val profileArguments = if (isVideoToolbox(videoCodec)) arrayOf("-profile:v", "main10") else emptyArray()
        // the frames are already Y'CbCr, they are only subsampled, or uploaded to the GPU
        val filters = filters + rotation.filters + input.filters.ifEmpty { listOf("format=${input.pixelFormat}") }
        val filterArguments = arrayOf("-vf", filters.joinToString(","))

        return hwaccelArguments + pixelFormatArguments + hdrColorArguments() + filterArguments +
                videoCodecArguments + profileArguments + qualityArguments(hardwareEncoder) + x265Parameters +
                tagArguments + userArguments
    }

    /**
     * The quality arguments of the encoder: a constant quality for the hardware encoders, a constant rate factor and
     * preset for libx265
     */
    private fun qualityArguments(hardwareEncoder: HardwareEncoder?): Array<String> =
        hardwareEncoder?.arguments(quality)?.toTypedArray()
            ?: ((constantRateFactor?.let { arrayOf("-crf", it.toString()) } ?: emptyArray()) +
                    (preset?.let { arrayOf("-preset", it) } ?: emptyArray()))

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

        val hardwareEncoder = hardwareEncoder()
        val videoCodec = encoder(hardwareEncoder)
        val requestedPixelFormat = if (fullChroma) "yuv444p" else pixelFormat
        val input = hardwareEncoder?.input(requestedPixelFormat ?: "yuv420p") ?: EncoderInput(requestedPixelFormat, emptyList())

        // the filters are not changed, the arguments can be requested more than once
        val filters = filters + rotation.filters +
                (if (highPrecisionChroma) listOf("colorspace=bt709:iall=bt601-6-625:fast=1") else emptyList()) +
                input.filters

        @Suppress("DEPRECATION")
        val hwaccelArguments = hwaccel?.let { arrayOf("-hwaccel", it) } ?: emptyArray()
        val pixelFormatArguments = input.pixelFormat?.let { arrayOf("-pix_fmt", it) } ?: emptyArray()
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        val filterArguments = arrayOf("-vf", filters.joinToString(","))

        val arguments =
            hwaccelArguments + pixelFormatArguments + chromaArguments + filterArguments + videoCodecArguments +
                    qualityArguments(hardwareEncoder) + tagArguments + userArguments

        return arguments
    }
}

/**
 * Configure a h265 video profile
 */
fun ScreenRecorder.h265(configure : H265Profile.() -> Unit = {}) {
    profile = H265Profile().apply(configure)
}
