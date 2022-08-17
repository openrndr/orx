plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    api(project(":orx-noise"))
    implementation(libs.delaunator)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}