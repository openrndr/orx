plugins {
    org.openrndr.convention.`kotlin-jvm`
}

dependencies {
    implementation (libs.gson)
    implementation(project(":orx-fx"))
    implementation(project(":orx-jvm:orx-keyframer"))
    implementation(project(":orx-easing"))
    implementation(project(":orx-shader-phrases"))
    implementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.ffmpeg)
    demoImplementation(openrndrLibs.openrndr.filter)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}