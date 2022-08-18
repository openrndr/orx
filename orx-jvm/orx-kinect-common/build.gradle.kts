plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}
dependencies {
    api(project(":orx-depth-camera"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
}