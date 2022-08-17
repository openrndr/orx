plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(project(":orx-fx"))
    implementation(project(":orx-parameters"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-compositor"))
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.svg)
    demoImplementation(openrndrLibs.openrndr.ffmpeg)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}