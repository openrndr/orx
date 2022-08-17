plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-parameters"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-shader-phrases"))
    demoImplementation(libs.slf4j.simple)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.ffmpeg)
    demoImplementation(openrndrLibs.openrndr.svg)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}