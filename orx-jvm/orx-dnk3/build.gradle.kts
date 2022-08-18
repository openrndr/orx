plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.gson)
    implementation(project(":orx-fx"))
    implementation(project(":orx-jvm:orx-keyframer"))
    implementation(project(":orx-easing"))
    implementation(project(":orx-shader-phrases"))
    implementation(project(":orx-mesh-generators"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoImplementation(libs.openrndr.ffmpeg)
    demoImplementation(libs.openrndr.filter)
    demoRuntimeOnly(libs.openrndr.gl3.core)
}