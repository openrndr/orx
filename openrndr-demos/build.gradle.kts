plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-parameters"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-shader-phrases"))
    demoImplementation(libs.slf4j.simple)
    demoImplementation(libs.openrndr.ffmpeg)
    demoImplementation(libs.openrndr.svg)
}