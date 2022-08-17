plugins {
    org.openrndr.convention.`kotlin-jvm`
}

dependencies {
    implementation(project(":orx-parameters"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}