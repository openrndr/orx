plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
    id("org.openrndr.extra.convention.variant")
}
variants {
    val nativeLibs = listOf(libs.librealsense, sharedLibs.javacpp)

    val platforms = listOf(
        Triple(OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64, "windows-x86_64"),
        Triple(OperatingSystemFamily.MACOS, MachineArchitecture.X86_64, "macosx-x86_64"),
        Triple(OperatingSystemFamily.LINUX, MachineArchitecture.X86_64, "linux-x86_64"),
        Triple(OperatingSystemFamily.LINUX, MachineArchitecture.ARM64, "linux-arm64"),
    )

    for ((os, arch, classifier) in platforms) {
        platform(os, arch) {
            dependencies {
                nativeLibs.forEach {
                    runtimeOnly(it.get().withClassifier(classifier))
                }
            }
        }
    }
}
dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    api(libs.librealsense)
    demoRuntimeOnly(project(":orx-jvm:orx-realsense2"))
    demoImplementation(project(":orx-color"))
}