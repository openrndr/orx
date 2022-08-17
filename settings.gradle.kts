import org.gradle.internal.os.OperatingSystem

rootProject.name = "orx"

@Suppress("INACCESSIBLE_TYPE")
// This is equivalent to `gradle.ext` https://stackoverflow.com/a/65377323/17977931
val openrndrClassifier: String by (gradle as ExtensionAware).extra(
    "natives-" + when (val os = OperatingSystem.current()) {
        OperatingSystem.WINDOWS -> "windows"
        OperatingSystem.LINUX -> "linux-x64"
        OperatingSystem.MAC_OS -> "macos"
        else -> error("Unsupported operating system: $os")
    }
)

val openrndrVersion =
    (extra.properties["OPENRNDR.version"] as String? ?: System.getenv("OPENRNDR_VERSION"))?.replace("v", "")
        ?: "0.5.1-SNAPSHOT"

dependencyResolutionManagement {
    versionCatalogs {
        create("openrndrLibs") {
            version("openrndr", openrndrVersion)

            library("openrndr-application", "org.openrndr", "openrndr-application").versionRef("openrndr")
            library("openrndr-extensions", "org.openrndr", "openrndr-extensions").versionRef("openrndr")
            library("openrndr-math", "org.openrndr", "openrndr-math").versionRef("openrndr")
            library("openrndr-shape", "org.openrndr", "openrndr-shape").versionRef("openrndr")
            library("openrndr-draw", "org.openrndr", "openrndr-draw").versionRef("openrndr")
            library("openrndr-event", "org.openrndr", "openrndr-event").versionRef("openrndr")
            library("openrndr-filter", "org.openrndr", "openrndr-filter").versionRef("openrndr")
            library("openrndr-dialogs", "org.openrndr", "openrndr-dialogs").versionRef("openrndr")
            library("openrndr-ffmpeg", "org.openrndr", "openrndr-ffmpeg").versionRef("openrndr")
            library("openrndr-svg", "org.openrndr", "openrndr-svg").versionRef("openrndr")
            library("openrndr-gl3-core", "org.openrndr", "openrndr-gl3").versionRef("openrndr")
        }
    }
}

include(
    listOf(
        "openrndr-demos",
        "orx-jvm:orx-boofcv",
        "orx-camera",
        "orx-jvm:orx-chataigne",
        "orx-color",
        "orx-compositor",
        "orx-compute-graph",
        "orx-compute-graph-nodes",
        "orx-jvm:orx-dnk3",
        "orx-easing",
        "orx-jvm:orx-file-watcher",
        "orx-parameters",
        "orx-fx",
        "orx-jvm:orx-git-archiver",
        "orx-jvm:orx-git-archiver-gradle",
        "orx-glslify",
        "orx-gradient-descent",
        "orx-hash-grid",
        "orx-integral-image",
        "orx-interval-tree",
        "orx-jumpflood",
        "orx-jvm:orx-gui",
        "orx-image-fit",
        "orx-kdtree",
        "orx-jvm:orx-keyframer",
        "orx-mesh-generators",
        "orx-jvm:orx-minim",
        "orx-jvm:orx-kotlin-parser",
        "orx-jvm:orx-midi",
        "orx-no-clear",
        "orx-noise",
        "orx-obj-loader",
        "orx-jvm:orx-olive",
        "orx-jvm:orx-osc",
        "orx-palette",
        "orx-jvm:orx-panel",
        "orx-jvm:orx-poisson-fill",
        "orx-quadtree",
        "orx-jvm:orx-rabbit-control",
        "orx-jvm:orx-realsense2",
        "orx-jvm:orx-realsense2-natives-linux-x64",
        "orx-jvm:orx-realsense2-natives-macos",
        "orx-jvm:orx-realsense2-natives-windows",
        "orx-jvm:orx-runway",
        "orx-shader-phrases",
        "orx-shade-styles",
        "orx-shapes",
        "orx-jvm:orx-syphon",
        "orx-temporal-blur",
        "orx-jvm:orx-tensorflow",
        "orx-jvm:orx-tensorflow-gpu-natives-linux-x64",
        "orx-jvm:orx-tensorflow-gpu-natives-windows",
        "orx-jvm:orx-tensorflow-natives-linux-x64",
        "orx-jvm:orx-tensorflow-natives-macos",
        "orx-jvm:orx-tensorflow-natives-windows",
        "orx-timer",
        "orx-time-operators",
        "orx-jvm:orx-triangulation",
        "orx-jvm:orx-kinect-common",
        "orx-jvm:orx-kinect-v1",
        "orx-jvm:orx-kinect-v1-natives-linux-arm64",
        "orx-jvm:orx-kinect-v1-natives-linux-x64",
        "orx-jvm:orx-kinect-v1-natives-macos",
        "orx-jvm:orx-kinect-v1-natives-windows",
        "orx-jvm:orx-kinect-v1-demo",
        "orx-jvm:orx-video-profiles",
        "orx-depth-camera",
        "orx-jvm:orx-depth-camera-calibrator"
    )
)
