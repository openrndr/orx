package org.openrndr.extra.convention

import org.gradle.internal.component.external.model.DefaultModuleComponentSelector

addHostMachineAttributesToRuntimeConfigurations()

val openrndrVersion: String =
    (extra.properties["OPENRNDR.version"] as String? ?: System.getenv("OPENRNDR_VERSION"))?.removePrefix("v")
        ?: "0.5.1-SNAPSHOT"

 val openrndrModules = listOf(
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
)

configurations.all {
    for (module in openrndrModules) {
        resolutionStrategy.force("org.openrndr:$module:$openrndrVersion")
    }
}

dependencies {
    components {
        all<LwjglRule>()
        withModule<FFmpegRule>("org.bytedeco:javacpp")
        withModule<FFmpegRule>("org.bytedeco:ffmpeg")
    }
}