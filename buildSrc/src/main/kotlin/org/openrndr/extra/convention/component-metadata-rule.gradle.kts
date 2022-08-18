package org.openrndr.extra.convention

addHostMachineAttributesToRuntimeConfigurations()

val openrndrVersion: String =
    (extra.properties["OPENRNDR.version"] as String? ?: System.getenv("OPENRNDR_VERSION"))?.removePrefix("v")
        ?: "0.5.1-SNAPSHOT"

val openrndrModules = arrayOf(
    "openrndr-application",
    "openrndr-extensions",
    "openrndr-math",
    "openrndr-shape",
    "openrndr-draw",
    "openrndr-event",
    "openrndr-filter",
    "openrndr-dialogs",
    "openrndr-ffmpeg",
    "openrndr-svg",
    "openrndr-gl3"
).map { "org.openrndr:$it:$openrndrVersion" }.toTypedArray()

configurations.all {
    resolutionStrategy.force(*openrndrModules)
}

dependencies {
    components {
        all<LwjglRule>()
        withModule<FFmpegRule>("org.bytedeco:javacpp")
        withModule<FFmpegRule>("org.bytedeco:ffmpeg")
    }
}