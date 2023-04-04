plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(project(":orx-fx"))
    implementation(project(":orx-noise"))
    implementation(libs.openrndr.filter)
    demoRuntimeOnly(libs.slf4j.simple)
}