plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.ffmpeg)
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoImplementation(libs.openrndr.ffmpeg)
    demoRuntimeOnly(libs.openrndr.gl3.core)
}