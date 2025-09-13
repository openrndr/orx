plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    api(libs.librealsense)
    demoRuntimeOnly(project(":orx-jvm:orx-realsense2-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    demoImplementation(project(":orx-color"))
}