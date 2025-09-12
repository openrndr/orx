plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.gson)
    implementation(project(":orx-fx"))
    implementation(project(":orx-jvm:orx-keyframer"))
    implementation(project(":orx-easing"))
    implementation(project(":orx-shader-phrases"))
    implementation(project(":orx-mesh-generators"))
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.coroutines)
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-shader-phrases"))
    demoImplementation(openrndr.ffmpeg)
    demoImplementation(openrndr.filter)
}