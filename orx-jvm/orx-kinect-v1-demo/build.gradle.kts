dependencies {
    implementation(project(":orx-jvm:orx-kinect-v1"))
    implementation(project(":orx-jvm:orx-depth-camera-calibrator"))
    implementation(project(":orx-fx"))
    implementation(project(":orx-jvm:orx-gui"))
    runtimeOnly(project(":orx-jvm:orx-kinect-v1-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    runtimeOnly(libs.openrndr.gl3.core)
    runtimeOnly(libs.openrndr.gl3.natives)
    runtimeOnly(libs.logback.classic)
}
