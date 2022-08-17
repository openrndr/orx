plugins {
    org.openrndr.convention.`kotlin-jvm`
}

dependencies {
    api(project(":orx-jvm:orx-osc"))
    implementation(libs.gson)
    demoImplementation(openrndrLibs.openrndr.application)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
    demoRuntimeOnly(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.ffmpeg)
    demoImplementation(project(":orx-fx"))
}