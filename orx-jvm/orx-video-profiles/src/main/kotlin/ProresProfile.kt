package org.openrndr.extra.videoprofiles

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

    override fun arguments(): Array<String> {
        val vcodec = arrayOf("-vcodec", codec)
        val profile = arrayOf("-profile:v", profile.argument)
        val filters = arrayOf("-vf", "vflip")
        val audio = arrayOf("-an")
        return vcodec + profile + filters + audio
    }
}