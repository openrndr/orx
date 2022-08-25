package org.openrndr.extra.convention

addHostMachineAttributesToRuntimeConfigurations()

val openrndrVersion: String =
    (extra.properties["OPENRNDR.version"] as String? ?: System.getenv("OPENRNDR_VERSION"))?.removePrefix("v")
        ?: "0.5.1-SNAPSHOT"

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.openrndr") useVersion(openrndrVersion)
    }
}

dependencies {
    components {
        all<LwjglRule>()
        withModule<FFmpegRule>("org.bytedeco:javacpp")
        withModule<FFmpegRule>("org.bytedeco:ffmpeg")
    }
}