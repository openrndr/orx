plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(project(":orx-jvm:orx-kinect-v1"))
    implementation(project(":orx-jvm:orx-depth-camera-calibrator"))
    implementation(project(":orx-fx"))
    implementation(project(":orx-jvm:orx-gui"))
    runtimeOnly(project(":orx-jvm:orx-kinect-v1"))
    runtimeOnly(openrndr.gl3)
}