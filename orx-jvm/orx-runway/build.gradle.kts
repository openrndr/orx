plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.gson)
    demoImplementation(libs.openrndr.ffmpeg)
    demoImplementation(project(":orx-fx"))
}