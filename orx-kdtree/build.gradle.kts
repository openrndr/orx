plugins {
    org.openrndr.convention.`kotlin-jvm`
}

dependencies {
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}