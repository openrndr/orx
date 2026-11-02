package org.openrndr.extra.videoprofiles

import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoRotation
import org.openrndr.ffmpeg.VideoWriterProfile

class ProresProfile : VideoWriterProfile() {
    enum class Profile(val argument:String) {
        PROXY("0"),
        LT("1"),
        SQ("2"),
        HQ("3"),
        HQ4444("4444")
    }

    override val fileExtension: String = "mov"
    var profile = Profile.SQ

    var codec = "prores_ks"

    /**
     * Encode with the hardware ProRes encoder where one is supported: VideoToolbox on macOS. Encoding falls back to
     * software when the hardware does not support ProRes. On other platforms the encoder is not changed.
     */
    fun hardwareAcceleration() {
        if (isMacOS) {
            codec = "prores_videotoolbox"
        }
    }

    val filters = mutableListOf("vflip")

    /**
     * Rotates the frames, a 90 degree rotation swaps the width and height of the video
     */
    var rotation = VideoRotation.NONE

    /**
     * The pixel format the frames are encoded from: 10 bit 4:2:2, or 4:4:4 with alpha for [Profile.HQ4444]. The
     * VideoToolbox encoder takes them semi-planar (p210le) or packed with 16 bits per component (ayuv64le).
     */
    private fun pixelFormat(): String {
        val alpha = profile == Profile.HQ4444
        return if (isVideoToolbox(codec)) {
            if (alpha) "ayuv64le" else "p210le"
        } else {
            if (alpha) "yuva444p10le" else "yuv422p10le"
        }
    }

    override fun arguments(): Array<String> {
        val vcodec = arrayOf("-vcodec", codec)
        val profile = arrayOf("-profile:v", profile.argument)
        val pixelFormat = pixelFormat()
        // the frames are converted to Y'CbCr with the BT.709 matrix here and tagged as such, the encoders would
        // otherwise convert them with a matrix that does not match the tags of the video. The tags of the frames are
        // set as well, the encoders take the primaries and transfer from the frames.
        val conversion = "scale=out_color_matrix=bt709:out_range=tv:flags=accurate_rnd+full_chroma_int," +
                "format=$pixelFormat,setparams=color_primaries=bt709:color_trc=bt709:colorspace=bt709:range=tv"
        val filters = arrayOf("-vf", (filters + rotation.filters + conversion).joinToString(","))
        val colorArguments = arrayOf(
            "-pix_fmt", pixelFormat,
            "-color_primaries", "bt709", "-color_trc", "bt709", "-colorspace", "bt709", "-color_range", "tv"
        )
        val audio = arrayOf("-an")
        val encoder = if (isVideoToolbox(codec)) videoToolboxArguments else emptyArray()
        return vcodec + profile + encoder + filters + colorArguments + audio
    }
}

/**
 * Configure a Prores video profile
 */
fun ScreenRecorder.prores(configure : ProresProfile.() -> Unit = {}) {
    profile = ProresProfile().apply(configure)
}