package org.openrndr.convention

addHostMachineAttributesToRuntimeConfigurations()

dependencies {
    components {
        all<LwjglRule>()
        withModule<FFmpegRule>("org.bytedeco:javacpp")
        withModule<FFmpegRule>("org.bytedeco:ffmpeg")
    }
}