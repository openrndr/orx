plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(libs.jgit)
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(openrndr.ffmpeg)
    demoImplementation(openrndr.filter)
}