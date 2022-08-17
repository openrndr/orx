plugins {
    org.openrndr.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.jgit)
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.ffmpeg)
    demoImplementation(openrndrLibs.openrndr.filter)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}