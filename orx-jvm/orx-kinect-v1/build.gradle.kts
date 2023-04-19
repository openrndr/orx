plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.coroutines)
    api(project(":orx-jvm:orx-kinect-common"))
    api(libs.libfreenect)
    demoImplementation(project(":orx-jvm:orx-kinect-v1"))
    demoImplementation(project(":orx-jvm:orx-depth-camera-calibrator"))
    demoImplementation(project(":orx-fx"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoRuntimeOnly(project(":orx-jvm:orx-kinect-v1-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    demoRuntimeOnly(libs.slf4j.simple)
}
