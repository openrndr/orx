plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.gson)
    implementation(project(":orx-noise"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
}