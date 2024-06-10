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

include(
    listOf(
        "openrndr-demos",
        "orx-jvm:orx-boofcv",
        "orx-camera",
        "orx-jvm:orx-chataigne",
        "orx-color",
        "orx-composition",
        "orx-compositor",
        "orx-compute-graph",
        "orx-compute-graph-nodes",
        "orx-delegate-magic",
        "orx-jvm:orx-dnk3",
        "orx-easing",
        "orx-envelopes",
        "orx-expression-evaluator",
        "orx-expression-evaluator-typed",
        "orx-fcurve",
        "orx-fft",
        "orx-jvm:orx-file-watcher",
        "orx-parameters",
        "orx-fx",
        "orx-jvm:orx-git-archiver",
        "orx-jvm:orx-git-archiver-gradle",
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
        "orx-marching-squares",
        "orx-jvm:orx-olive",
        "orx-jvm:orx-osc",
        "orx-palette",
        "orx-property-watchers",
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
        "orx-svg",
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
        "orx-triangulation",
        "orx-jvm:orx-kinect-common",
        "orx-jvm:orx-kinect-v1",
        "orx-jvm:orx-kinect-v1-natives-linux-arm64",
        "orx-jvm:orx-kinect-v1-natives-linux-x64",
        "orx-jvm:orx-kinect-v1-natives-macos",
        "orx-jvm:orx-kinect-v1-natives-windows",
        "orx-jvm:orx-kinect-v1-demo",
        "orx-jvm:orx-video-profiles",
        "orx-depth-camera",
        "orx-jvm:orx-depth-camera-calibrator",
        "orx-view-box",
        "orx-text-writer",
        "orx-turtle"
    )
)