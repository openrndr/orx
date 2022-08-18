plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.ffmpeg)
    demoImplementation(libs.openrndr.ffmpeg)
}