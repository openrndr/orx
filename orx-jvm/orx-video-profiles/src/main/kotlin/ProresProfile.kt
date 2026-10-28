package org.openrndr.extra.videoprofiles

import org.openrndr.ffmpeg.ScreenRecorder
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

    override fun arguments(): Array<String> {
        val vcodec = arrayOf("-vcodec", codec)
        val profile = arrayOf("-profile:v", profile.argument)
        val filters = arrayOf("-vf", filters.joinToString(","))
        val audio = arrayOf("-an")
        val encoder = if (isVideoToolbox(codec)) videoToolboxArguments else emptyArray()
        return vcodec + profile + encoder + filters + audio
    }
}

/**
 * Configure a Prores video profile
 */
fun ScreenRecorder.prores(configure : ProresProfile.() -> Unit = {}) {
    profile = ProresProfile().apply(configure)
}