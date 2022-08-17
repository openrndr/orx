plugins {
    org.openrndr.convention.`kotlin-jvm`
}

dependencies {
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-shapes"))
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}