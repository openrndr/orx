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
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoImplementation(libs.openrndr.ffmpeg)
    demoImplementation(libs.openrndr.svg)
    demoRuntimeOnly(libs.openrndr.gl3.core)
}