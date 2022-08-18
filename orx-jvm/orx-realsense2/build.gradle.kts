plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    api(libs.librealsense)
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoRuntimeOnly(project(":orx-jvm:orx-realsense2-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    demoRuntimeOnly(libs.openrndr.gl3.core)
}