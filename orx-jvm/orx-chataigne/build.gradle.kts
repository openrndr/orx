plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    api(project(":orx-jvm:orx-osc"))
    implementation(libs.gson)
    demoImplementation(openrndr.ffmpeg)
    demoImplementation(project(":orx-fx"))
}