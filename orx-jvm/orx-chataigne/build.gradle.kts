plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    api(project(":orx-jvm:orx-osc"))
    implementation(libs.gson)
    demoImplementation(libs.openrndr.application)
    demoRuntimeOnly(libs.openrndr.gl3.core)
    demoRuntimeOnly(libs.openrndr.extensions)
    demoImplementation(libs.openrndr.ffmpeg)
    demoImplementation(project(":orx-fx"))
}