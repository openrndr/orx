plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.jgit)
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(libs.openrndr.ffmpeg)
    demoImplementation(libs.openrndr.filter)
}