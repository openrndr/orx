plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(project(":orx-jvm:orx-kinect-v1"))
    implementation(project(":orx-jvm:orx-depth-camera-calibrator"))
    implementation(project(":orx-fx"))
    implementation(project(":orx-jvm:orx-gui"))
    runtimeOnly(project(":orx-jvm:orx-kinect-v1-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    runtimeOnly(libs.openrndr.gl3.core)
    runtimeOnly(libs.logback.classic)
}