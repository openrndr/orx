package org.openrndr.extra.convention

addHostMachineAttributesToRuntimeConfigurations()

dependencies {
    components {
        all<LwjglRule>()
        withModule<FFmpegRule>("org.bytedeco:javacpp")
        withModule<FFmpegRule>("org.bytedeco:ffmpeg")
    }
}