plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(project(":orx-fx"))
    implementation(project(":orx-parameters"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-compositor"))
    demoImplementation(libs.openrndr.svg)
    demoImplementation(libs.openrndr.ffmpeg)
}