plugins {
    org.openrndr.convention.`kotlin-jvm`
}

dependencies {
    api(libs.librealsense)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(project(":orx-jvm:orx-realsense2-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}