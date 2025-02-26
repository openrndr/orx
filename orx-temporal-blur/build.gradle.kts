plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(project(":orx-color"))
    implementation(project(":orx-noise"))
    implementation(project(":orx-fx"))
    implementation(libs.openrndr.filter)
}