plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    api(libs.librealsense)
    demoRuntimeOnly(project(":orx-jvm:orx-realsense2-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    demoImplementation(project(":orx-color"))
}