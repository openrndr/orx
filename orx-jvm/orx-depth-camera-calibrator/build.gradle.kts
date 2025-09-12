plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(project(":orx-fx"))
    api(project(":orx-depth-camera"))
    api(project(":orx-jvm:orx-gui"))
}