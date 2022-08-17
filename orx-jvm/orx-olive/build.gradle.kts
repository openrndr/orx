plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(project(":orx-jvm:orx-file-watcher"))
    implementation(project(":orx-jvm:orx-kotlin-parser"))
    implementation(libs.kotlin.scriptingJvm)
    implementation(libs.kotlin.scriptingJvmHost)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.scriptingJSR223)
    demoImplementation(project(":orx-camera"))
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}