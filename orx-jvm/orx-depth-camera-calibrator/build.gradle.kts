plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(project(":orx-fx"))
    api(project(":orx-depth-camera"))
    api(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-jvm:orx-kinect-v1"))
    demoRuntimeOnly(project(":orx-jvm:orx-kinect-v1-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    demoRuntimeOnly(libs.slf4j.simple)
}
