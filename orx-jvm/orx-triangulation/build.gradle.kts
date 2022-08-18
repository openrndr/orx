plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    api(project(":orx-noise"))
    implementation(libs.openrndr.shape)
    implementation(libs.openrndr.math)
    implementation(libs.delaunator)
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoRuntimeOnly(libs.openrndr.gl3.core)
}